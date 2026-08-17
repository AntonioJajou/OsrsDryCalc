package com.example.drycalc;

import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.drycalc.databinding.FragmentFirstBinding;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(4);

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = binding.usernameInput.getText().toString().trim();
                if (username.isEmpty()) {
                    binding.statusText.setText("Enter a RuneScape username first.");
                    return;
                }
                loadBossLog(username);
            }
        });
    }

    private void loadBossLog(String username) {
        binding.buttonFirst.setEnabled(false);
        binding.loading.setVisibility(View.VISIBLE);
        binding.statusText.setText("Loading kill counts and collection-log drops…");
        binding.resultsContainer.removeAllViews();

        networkExecutor.execute(() -> {
            try {
                String encodedUsername = URLEncoder.encode(username, "UTF-8").replace("+", "%20");
                JSONObject account = new JSONObject(fetchJson("https://api.runeprofile.com/v1/accounts/" + encodedUsername + "/full"));
                JSONObject hiscores = new JSONObject(fetchJson("https://secure.runescape.com/m=hiscore_oldschool/index_lite.json?player=" + encodedUsername));
                String result = formatRateReport(account, hiscores);
                if (getActivity() != null) getActivity().runOnUiThread(() -> showResult(result, account, hiscores));
            } catch (Exception error) {
                if (getActivity() != null) getActivity().runOnUiThread(() ->
                        showResult("Could not load RuneProfile data. Check your connection and try again.\n\n" + error.getMessage(), null, null));
            }
        });
    }

    private String fetchJson(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "BossLogTracker/1.0");
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        } finally {
            connection.disconnect();
        }
        return response.toString();
    }

    private String formatRateReport(JSONObject account, JSONObject hiscores) throws Exception {
        JSONObject bossTab = null;
        JSONArray tabs = account.getJSONObject("collectionLog").getJSONArray("tabs");
        for (int i = 0; i < tabs.length(); i++) {
            if ("Bosses".equals(tabs.getJSONObject(i).getString("name"))) {
                bossTab = tabs.getJSONObject(i);
                break;
            }
        }
        if (bossTab == null) throw new Exception("Boss collection log was not found.");

        StringBuilder output = new StringBuilder();
        Map<String, Integer> killCounts = new HashMap<>();
        JSONArray activities = hiscores.getJSONArray("activities");
        for (int i = 0; i < activities.length(); i++) {
            JSONObject activity = activities.getJSONObject(i);
            killCounts.put(activity.getString("name"), activity.getInt("score"));
        }
        JSONArray pages = bossTab.getJSONArray("pages");
        double completion = bossTab.getInt("obtained") * 100.0 / bossTab.getInt("total");
        output.append(account.getString("username")).append(" — Boss log: ")
                .append(bossTab.getInt("obtained")).append("/").append(bossTab.getInt("total"))
                .append(" unlocked\n")
                .append(String.format(Locale.US, "Boss-log completion: %.1f%%", completion)).append("\n")
                .append(accountRateSummary(pages, killCounts));
        return output.toString();
    }

    private void appendCollectionLog(StringBuilder output, JSONArray pages, Map<String, Integer> killCounts) throws Exception {
        for (int i = 0; i < pages.length(); i++) {
            JSONObject page = pages.getJSONObject(i);
            StringBuilder unlockedItems = new StringBuilder();
            JSONArray items = page.getJSONArray("items");
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.getJSONObject(j);
                int quantity = item.getInt("quantity");
                if (quantity > 0) {
                    String itemName = item.getString("name");
                    unlockedItems.append("\n• ").append(itemName).append(" ×").append(quantity);
                    String rate = rateDescription(page.getString("name"), itemName, quantity, killCounts);
                    if (rate != null) unlockedItems.append(" — ").append(rate);
                }
            }
            if (unlockedItems.length() == 0) continue;

            String pageName = page.getString("name");
            output.append("\n\n").append(pageName).append(" — ")
                    .append(collectionLogKills(pageName, killCounts))
                    .append(unlockedItems);
        }
    }

    private void renderCollectionLog(JSONObject account, JSONObject hiscores) {
        try {
            JSONObject bossTab = null;
            JSONArray tabs = account.getJSONObject("collectionLog").getJSONArray("tabs");
            for (int i = 0; i < tabs.length(); i++) {
                if ("Bosses".equals(tabs.getJSONObject(i).getString("name"))) bossTab = tabs.getJSONObject(i);
            }
            if (bossTab == null) return;

            Map<String, Integer> killCounts = new HashMap<>();
            JSONArray activities = hiscores.getJSONArray("activities");
            for (int i = 0; i < activities.length(); i++) {
                JSONObject activity = activities.getJSONObject(i);
                killCounts.put(activity.getString("name"), activity.getInt("score"));
            }

            JSONArray pages = bossTab.getJSONArray("pages");
            for (int i = 0; i < pages.length(); i++) {
                JSONObject page = pages.getJSONObject(i);
                JSONArray items = page.getJSONArray("items");
                boolean hasDrops = false;
                for (int j = 0; j < items.length(); j++) if (items.getJSONObject(j).getInt("quantity") > 0) hasDrops = true;
                if (!hasDrops) continue;

                String boss = page.getString("name");
                String totalSummary = bossRateSummary(boss, items, killCounts);
                MaterialCardView card = new MaterialCardView(requireContext());
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, dp(10), 0, 0);
                card.setLayoutParams(cardParams);
                card.setCardBackgroundColor(getResources().getColor(R.color.osrs_parchment));
                card.setStrokeColor(getResources().getColor(R.color.osrs_gold_dark));
                card.setStrokeWidth(dp(1));
                card.setRadius(dp(10));
                card.setCardElevation(dp(2));

                LinearLayout cardLayout = new LinearLayout(requireContext());
                cardLayout.setOrientation(LinearLayout.VERTICAL);
                cardLayout.setPadding(dp(12), dp(10), dp(12), dp(10));

                LinearLayout header = new LinearLayout(requireContext());
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView heading = new TextView(requireContext());
                heading.setText(boss + " — " + collectionLogKills(boss, killCounts) + "\n" + totalSummary);
                heading.setTextSize(19);
                heading.setTypeface(Typeface.DEFAULT_BOLD);
                heading.setTextColor(getResources().getColor(R.color.osrs_gold_dark));
                heading.setPadding(0, 0, 0, dp(2));
                heading.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                header.addView(heading);

                TextView chevron = new TextView(requireContext());
                chevron.setText("⌄");
                chevron.setTextSize(30);
                chevron.setTextColor(getResources().getColor(R.color.osrs_gold_dark));
                chevron.setGravity(android.view.Gravity.CENTER);
                chevron.setContentDescription("Expand boss drops");
                chevron.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
                header.addView(chevron);
                cardLayout.addView(header);

                LinearLayout dropsContainer = new LinearLayout(requireContext());
                dropsContainer.setOrientation(LinearLayout.VERTICAL);
                dropsContainer.setVisibility(View.GONE);
                cardLayout.addView(dropsContainer);
                header.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean isCollapsed = dropsContainer.getVisibility() != View.VISIBLE;
                        dropsContainer.setVisibility(isCollapsed ? View.VISIBLE : View.GONE);
                        chevron.setText(isCollapsed ? "⌃" : "⌄");
                        chevron.setContentDescription(isCollapsed ? "Collapse boss drops" : "Expand boss drops");
                    }
                });

                for (int j = 0; j < items.length(); j++) {
                    JSONObject item = items.getJSONObject(j);
                    int quantity = item.getInt("quantity");
                    if (quantity <= 0) continue;
                    addItemRow(dropsContainer, boss, item.getInt("id"), item.getString("name"), quantity, killCounts);
                }
                card.addView(cardLayout);
                binding.resultsContainer.addView(card);
            }
        } catch (Exception error) {
            binding.statusText.append("\n\nCould not format collection log: " + error.getMessage());
        }
    }

    private void addItemRow(LinearLayout parent, String boss, int itemId, String itemName, int quantity, Map<String, Integer> killCounts) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        row.setMinimumHeight(dp(72));

        ImageView icon = new ImageView(requireContext());
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(55), dp(55)));
        icon.setContentDescription(itemName);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(icon);

        TextView label = new TextView(requireContext());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        labelParams.setMargins(dp(12), 0, 0, 0);
        label.setLayoutParams(labelParams);
        String rate = rateDescription(boss, itemName, quantity, killCounts);
        label.setText(itemName + " ×" + quantity + (rate == null ? "" : "\nRate: " + rate));
        label.setTextSize(16);
        label.setTextColor(getResources().getColor(R.color.osrs_ink));
        row.addView(label);
        parent.addView(row);
        loadItemIcon(icon, itemId);
    }

    private String bossRateSummary(String boss, JSONArray items, Map<String, Integer> killCounts) throws Exception {
        RateTotals totals = new RateTotals();
        if ("Barrows Chests".equals(boss)) {
            int pieces = countBarrowsPieces(items);
            int chests = killCounts.containsKey("Barrows Chests") ? killCounts.get("Barrows Chests") : 0;
            totals.add(pieces, chests / 14.57, 14.57);
            return totals.summary("Weighted total");
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            int quantity = item.getInt("quantity");
            if (quantity <= 0) continue;
            addRarityWeightedRate(totals, boss, item.getString("name"), quantity, killCounts);
        }
        return totals.summary("Weighted total");
    }

    private String accountRateSummary(JSONArray pages, Map<String, Integer> killCounts) throws Exception {
        RateTotals totals = new RateTotals();
        for (int i = 0; i < pages.length(); i++) {
            JSONObject page = pages.getJSONObject(i);
            String boss = page.getString("name");
            JSONArray items = page.getJSONArray("items");
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.getJSONObject(j);
                int quantity = item.getInt("quantity");
                if (quantity <= 0) continue;
                addRarityWeightedRate(totals, boss, item.getString("name"), quantity, killCounts);
            }
        }
        return totals.summary("Mapped weighted rate");
    }

    private void addRarityWeightedRate(RateTotals totals, String boss, String item, int actual,
                                       Map<String, Integer> killCounts) {
        String rate = rateDescription(boss, item, actual, killCounts);
        double expected = expectedFromRate(rate);
        int kills = killsForRateWeight(boss, killCounts);
        if (expected > 0 && kills > 0) totals.add(actual, expected, kills / expected);
    }

    private int killsForRateWeight(String boss, Map<String, Integer> killCounts) {
        if ("Kree'arra".equals(boss)) return value(killCounts, "Kree'Arra");
        if ("The Gauntlet".equals(boss)) return value(killCounts, "The Gauntlet") + value(killCounts, "The Corrupted Gauntlet");
        return value(killCounts, boss);
    }

    private int value(Map<String, Integer> values, String key) {
        return values.containsKey(key) ? values.get(key) : 0;
    }

    private static class RateTotals {
        private double weightedRate;
        private double totalWeight;

        void add(int actual, double expected, double weight) {
            if (expected <= 0 || weight <= 0) return;
            weightedRate += (actual / expected) * weight;
            totalWeight += weight;
        }

        String summary(String label) {
            if (totalWeight == 0) return label + ": special calculation needed";
            long percentage = Math.round(weightedRate / totalWeight * 100);
            return label + ": " + percentage + "% " + (percentage < 100 ? "dry" : "spooned");
        }
    }

    private int countBarrowsPieces(JSONArray items) throws Exception {
        int pieces = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (isBarrowsItem(item.getString("name"))) pieces += item.getInt("quantity");
        }
        return pieces;
    }

    private String combinedRateSummary(int actual, double expected) {
        if (expected <= 0) return "Total rate: special calculation needed";
        long percentage = Math.round(actual / expected * 100);
        return "Total: " + percentage + "% " + (percentage < 100 ? "dry" : "spooned");
    }

    private double expectedFromRate(String rate) {
        if (rate == null) return 0;
        int marker = rate.indexOf(" expected");
        if (marker < 0) return 0;
        try {
            return Double.parseDouble(rate.substring(0, marker));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void loadItemIcon(ImageView icon, int itemId) {
        networkExecutor.execute(() -> {
            try {
                URL url = new URL("https://cdn.runeprofile.com/item/" + itemId + ".png");
                Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (bitmap != null && getActivity() != null) getActivity().runOnUiThread(() -> icon.setImageBitmap(bitmap));
            } catch (Exception ignored) {
                // Leave the icon blank if RuneProfile has no image for this item ID.
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private String rateDescription(String boss, String item, int actual, Map<String, Integer> killCounts) {
        double denominator = 0;
        String hiscoreBoss = boss;
        if ("Abyssal Sire".equals(boss) && "Unsired".equals(item)) denominator = 100;
        else if ("Barrows Chests".equals(boss) && isBarrowsItem(item)) denominator = 350.14;
        else if ("Amoxliatl".equals(boss)) {
            if ("Pendant of ates (inert)".equals(item)) denominator = 25;
            else if ("Frozen tear".equals(item)) {
                int kills = killCounts.containsKey("Amoxliatl") ? killCounts.get("Amoxliatl") : 0;
                return expectedQuantityDescription(actual, kills * 5.55);
            }
        } else if ("Brutus".equals(boss)) {
            if ("Mooleta".equals(item)) denominator = 30;
            else if ("Bottomless milk bucket (empty)".equals(item)) denominator = 37.5;
            else if ("Cow slippers".equals(item)) denominator = 150;
            else if ("Beef".equals(item)) denominator = 1000;
        }
        else if ("Alchemical Hydra".equals(boss)) {
            if ("Hydra's claw".equals(item)) denominator = 1001;
            else if ("Hydra tail".equals(item)) denominator = 513;
            else if ("Hydra leather".equals(item)) denominator = 514;
            else if ("Hydra's eye".equals(item) || "Hydra's fang".equals(item) || "Hydra's heart".equals(item)) denominator = 181.1;
        } else if ("Araxxor".equals(boss)) {
            if ("Araxyte fang".equals(item) || "Noxious point".equals(item) || "Noxious blade".equals(item) || "Noxious pommel".equals(item)) denominator = 600;
            else if ("Araxyte head".equals(item)) denominator = 250;
            else if ("Jar of venom".equals(item)) denominator = 1500;
        } else if ("Cerberus".equals(boss)) {
            if ("Hellpuppy".equals(item)) denominator = 3000;
            else if ("Eternal crystal".equals(item) || "Pegasian crystal".equals(item) || "Primordial crystal".equals(item) || "Smouldering stone".equals(item)) denominator = 520;
        } else if ("Duke Sucellus".equals(boss) && ("Magus vestige".equals(item) || "Eye of the duke".equals(item))) denominator = 720;
        else if ("General Graardor".equals(boss)) {
            if ("Bandos hilt".equals(item)) denominator = 508;
            else if ("Bandos chestplate".equals(item) || "Bandos tassets".equals(item) || "Bandos boots".equals(item)) denominator = 381;
        } else if ("Commander Zilyana".equals(boss)) {
            if ("Armadyl crossbow".equals(item)) denominator = 508;
            else if ("Saradomin sword".equals(item)) denominator = 127;
            else if ("Saradomin's light".equals(item)) denominator = 254;
        } else if ("Kree'arra".equals(boss)) {
            hiscoreBoss = "Kree'Arra";
            if ("Armadyl hilt".equals(item)) denominator = 508;
            else if ("Armadyl helmet".equals(item) || "Armadyl chestplate".equals(item)) denominator = 381;
        } else if ("K'ril Tsutsaroth".equals(boss)) {
            if ("Zamorakian spear".equals(item)) denominator = 127;
            else if ("Zamorak hilt".equals(item)) denominator = 508;
        } else if ("Kalphite Queen".equals(boss) && "Jar of sand".equals(item)) denominator = 2000;
        else if ("Nex".equals(boss) && ("Nihil horn".equals(item) || "Torva platebody (damaged)".equals(item))) denominator = 258;
        else if ("Phantom Muspah".equals(boss)) {
            if ("Venator shard".equals(item)) denominator = 100;
            else if ("Ancient icon".equals(item)) denominator = 50;
        } else if ("Sarachnis".equals(boss)) {
            if ("Sarachnis cudgel".equals(item)) denominator = 384;
            else if ("Giant egg sac(full)".equals(item)) denominator = 20;
            else if ("Pristine spider silk".equals(item)) denominator = 50;
        } else if ("Scurrius".equals(boss) && "Scurrius' spine".equals(item)) denominator = 33;
        else if ("Maggot King".equals(boss) && "Elder venator fang".equals(item)) denominator = 340;
        else if ("Vardorvis".equals(boss) && "Ultor vestige".equals(item)) denominator = 1088;
        else if ("The Whisperer".equals(boss) && "Bellator vestige".equals(item)) denominator = 512;
        else if ("Yama".equals(boss)) {
            if ("Yami".equals(item)) denominator = 2500;
            else if ("Soulflame horn".equals(item)) denominator = 300;
            else if ("Oathplate helm".equals(item) || "Oathplate legs".equals(item)) denominator = 600;
            else if ("Dossier".equals(item)) denominator = 12.1;
            else if ("Forgotten lockbox".equals(item)) denominator = 33;
            else if ("Oathplate shards".equals(item)) {
                int kills = killCounts.containsKey("Yama") ? killCounts.get("Yama") : 0;
                return expectedQuantityDescription(actual, kills * 12.0 / 17.07);
            } else if ("Chasm teleport scroll".equals(item)) {
                int kills = killCounts.containsKey("Yama") ? killCounts.get("Yama") : 0;
                return expectedQuantityDescription(actual, kills * 6.0 * 4.0 / 95.11);
            } else if ("Barrel of demonic tallow (full)".equals(item)) denominator = 95.11 / 5.0;
        }
        else if ("Zulrah".equals(boss)) {
            if ("Tanzanite fang".equals(item) || "Magic fang".equals(item) || "Serpentine visage".equals(item)) denominator = 512;
            else if ("Tanzanite mutagen".equals(item) || "Magma mutagen".equals(item)) denominator = 6553.5;
            else if ("Pet Snakeling".equals(item)) denominator = 4000;
        }
        if ("The Gauntlet".equals(boss)) {
            int regular = killCounts.containsKey("The Gauntlet") ? killCounts.get("The Gauntlet") : 0;
            int corrupted = killCounts.containsKey("The Corrupted Gauntlet") ? killCounts.get("The Corrupted Gauntlet") : 0;
            double expected = 0;
            if ("Crystal armour seed".equals(item) || "Crystal weapon seed".equals(item)) {
                expected = regular / 120.0 + corrupted / 50.0;
            } else if ("Enhanced crystal weapon seed".equals(item)) {
                expected = regular / 2000.0 + corrupted / 400.0;
            }
            if (expected > 0) {
                long percentage = Math.round(actual / expected * 100);
                return String.format(Locale.US, "%.2f expected • %d%% %s", expected, percentage,
                        percentage < 100 ? "dry" : "spooned");
            }
        }
        if (denominator == 0) return null;
        int kills = killCounts.containsKey(hiscoreBoss) ? killCounts.get(hiscoreBoss) : 0;
        if (kills == 0) return null;
        double expected = kills / denominator;
        long percentage = Math.round(actual / expected * 100);
        return String.format(Locale.US, "%.2f expected • %d%% %s", expected, percentage,
                percentage < 100 ? "dry" : "spooned");
    }

    private boolean isBarrowsItem(String item) {
        return item.equals("Ahrim's hood") || item.equals("Ahrim's robetop") || item.equals("Ahrim's robeskirt") || item.equals("Ahrim's staff")
                || item.equals("Dharok's helm") || item.equals("Dharok's platebody") || item.equals("Dharok's platelegs") || item.equals("Dharok's greataxe")
                || item.equals("Guthan's helm") || item.equals("Guthan's platebody") || item.equals("Guthan's chainskirt") || item.equals("Guthan's warspear")
                || item.equals("Karil's coif") || item.equals("Karil's leathertop") || item.equals("Karil's leatherskirt") || item.equals("Karil's crossbow")
                || item.equals("Torag's helm") || item.equals("Torag's platebody") || item.equals("Torag's platelegs") || item.equals("Torag's hammers")
                || item.equals("Verac's helm") || item.equals("Verac's brassard") || item.equals("Verac's plateskirt") || item.equals("Verac's flail");
    }

    private String expectedQuantityDescription(int actual, double expected) {
        if (expected <= 0) return null;
        long percentage = Math.round(actual / expected * 100);
        return String.format(Locale.US, "%.2f expected • %d%% %s", expected, percentage,
                percentage < 100 ? "dry" : "spooned");
    }

    private String collectionLogKills(String boss, Map<String, Integer> killCounts) {
        if ("Dagannoth Kings".equals(boss)) {
            return "Prime " + count(killCounts, "Dagannoth Prime") + " • Rex " + count(killCounts, "Dagannoth Rex")
                    + " • Supreme " + count(killCounts, "Dagannoth Supreme") + " KC";
        }
        if ("Callisto and Artio".equals(boss)) return "Artio " + count(killCounts, "Artio") + " • Callisto " + count(killCounts, "Callisto") + " KC";
        if ("Venenatis and Spindel".equals(boss)) return "Spindel " + count(killCounts, "Spindel") + " • Venenatis " + count(killCounts, "Venenatis") + " KC";
        if ("Vet'ion and Calvar'ion".equals(boss)) return "Calvar'ion " + count(killCounts, "Calvar'ion") + " • Vet'ion " + count(killCounts, "Vet'ion") + " KC";
        if ("The Gauntlet".equals(boss)) return "Gauntlet " + count(killCounts, "The Gauntlet") + " • Corrupted " + count(killCounts, "The Corrupted Gauntlet") + " KC";
        if ("The Fight Caves".equals(boss)) return count(killCounts, "TzTok-Jad") + " KC";
        if ("Fortis Colosseum".equals(boss)) return count(killCounts, "Sol Heredit") + " KC";
        if ("Moons of Peril".equals(boss)) return count(killCounts, "Lunar Chests") + " KC";
        if ("Royal Titans".equals(boss)) return count(killCounts, "The Royal Titans") + " KC";
        if ("The Nightmare".equals(boss)) return "Nightmare " + count(killCounts, "Nightmare") + " • Phosani's " + count(killCounts, "Phosani's Nightmare") + " KC";
        if ("The Inferno".equals(boss)) return count(killCounts, "TzKal-Zuk") + " KC";
        if ("The Hueycoatl".equals(boss)) return count(killCounts, "The Hueycoatl") + " KC";

        String hiscoreBoss = boss;
        if ("Kree'arra".equals(boss)) hiscoreBoss = "Kree'Arra";
        if ("The Leviathan".equals(boss) || "The Whisperer".equals(boss)) return count(killCounts, hiscoreBoss) + " KC";
        if (killCounts.containsKey(hiscoreBoss)) return count(killCounts, hiscoreBoss) + " KC";
        return "KC not available in official HiScores";
    }

    private String count(Map<String, Integer> killCounts, String boss) {
        int kills = killCounts.containsKey(boss) ? killCounts.get(boss) : 0;
        return String.format(Locale.US, "%,d", kills);
    }

    private void addRate(StringBuilder output, String logBoss, String item, Map<String, Integer> drops,
                         Map<String, Integer> killCounts, String hiscoreBoss, String displayName, double denominator) {
        int kills = killCounts.containsKey(hiscoreBoss) ? killCounts.get(hiscoreBoss) : 0;
        int actual = drops.containsKey(logBoss + "|" + item) ? drops.get(logBoss + "|" + item) : 0;
        if (kills <= 0) return;
        double expected = kills / denominator;
        long percentage = Math.round(actual / expected * 100);
        String status = percentage < 100 ? " dry" : " spooned";
        output.append("\n\n").append(displayName).append(" — ").append(actual).append(" / ")
                .append(String.format(Locale.US, "%.2f", expected)).append(" expected — ")
                .append(percentage).append("%").append(status);
    }

    private void addCombinedRate(StringBuilder output, String logBoss, String displayName, Map<String, Integer> drops,
                                 Map<String, Integer> killCounts, String hiscoreBoss, double denominator, String... items) {
        int actual = 0;
        for (String item : items) {
            Integer quantity = drops.get(logBoss + "|" + item);
            actual += quantity == null ? 0 : quantity;
        }
        int kills = killCounts.containsKey(hiscoreBoss) ? killCounts.get(hiscoreBoss) : 0;
        addExpectedRate(output, displayName, actual, kills / denominator);
    }

    private void addExpectedRate(StringBuilder output, String displayName, Integer actualValue, double expected) {
        if (expected <= 0) return;
        int actual = actualValue == null ? 0 : actualValue;
        long percentage = Math.round(actual / expected * 100);
        String status = percentage < 100 ? " dry" : " spooned";
        output.append("\n\n").append(displayName).append(" — ").append(actual).append(" / ")
                .append(String.format(Locale.US, "%.2f", expected)).append(" expected — ")
                .append(percentage).append("%").append(status);
    }

    private void showResult(String result, JSONObject account, JSONObject hiscores) {
        if (binding == null) return;
        binding.loading.setVisibility(View.GONE);
        binding.buttonFirst.setEnabled(true);
        binding.statusText.setText(result);
        if (account != null && hiscores != null) renderCollectionLog(account, hiscores);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        networkExecutor.shutdown();
    }

}
