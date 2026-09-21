package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class VisitedCountriesManager {
    private static final String TAG = "VisitedCountriesMgr";
    private static final String PREFS = "blinkmod_settings";

    public static final class VisitedCountry {
        public final String code;
        public final String name;
        public final String flag;
        public final int citiesCount;
        public final long lastSeen;

        public VisitedCountry(String code, String name, String flag, int citiesCount, long lastSeen) {
            this.code = code == null ? "" : code.toUpperCase(Locale.ROOT);
            this.name = name == null ? "" : name;
            this.flag = (flag != null && !flag.isEmpty()) ? flag : getFlagForCode(this.code);
            this.citiesCount = Math.max(1, citiesCount);
            this.lastSeen = lastSeen > 0L ? lastSeen : System.currentTimeMillis();
        }

        public JSONObject toJSON() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("code", code);
                obj.put("name", name);
                obj.put("flag", flag);
                obj.put("cities_count", citiesCount);
                obj.put("last_seen", lastSeen);
                return obj;
            } catch (Throwable e) {
                return new JSONObject();
            }
        }

        public static VisitedCountry fromJSON(JSONObject obj) {
            if (obj == null) return null;
            String code = obj.optString("code", obj.optString("id", ""));
            String name = obj.optString("name", obj.optString("title", ""));
            String flag = obj.optString("flag", "");
            int cities = obj.optInt("cities_count", obj.optInt("citiesVisited", 1));
            long seen = obj.optLong("last_seen", System.currentTimeMillis());
            return new VisitedCountry(code, name, flag, cities, seen);
        }
    }

    public static final class CountryStats {
        public final int totalWorldCountries;
        public final int visitedCount;
        public final int missingCount;
        public final long lastSyncTime;
        public final String source;

        public CountryStats(int total, int visited, int missing, long lastSyncTime, String source) {
            this.totalWorldCountries = total;
            this.visitedCount = visited;
            this.missingCount = missing;
            this.lastSyncTime = lastSyncTime;
            this.source = source == null ? "" : source;
        }
    }

    public interface SyncCallback {
        void onSuccess(CountryStats stats);
        void onError(String message);
    }

    private static final Map<String, String> NAME_TO_CODE = new HashMap<String, String>();
    private static final Map<String, String> CODE_TO_NAME = new HashMap<String, String>();

    private static final String[] COUNTRY_DATA_0 = new String[] {
        "AD|Андорра|Andorra",
        "AE|Объединенные Арабские Эмираты|United Arab Emirates",
        "AF|Афганистан|Afghanistan",
        "AG|Антигуа и Барбуда|Antigua & Barbuda",
        "AI|Ангилья|Anguilla",
        "AL|Албания|Albania",
        "AM|Армения|Armenia",
        "AO|Ангола|Angola",
        "AR|Аргентина|Argentina",
        "AS|Американское Самоа|American Samoa",
        "AT|Австрия|Austria",
        "AU|Австралия|Australia",
        "AW|Аруба|Aruba",
        "AZ|Азербайджан|Azerbaijan",
        "BA|Босния и Герцеговина|Bosnia & Herzegovina",
        "BB|Барбадос|Barbados",
        "BD|Бангладеш|Bangladesh",
        "BE|Бельгия|Belgium",
        "BF|Буркина-Фасо|Burkina Faso",
        "BG|Болгария|Bulgaria",
        "BH|Бахрейн|Bahrain",
        "BI|Бурунди|Burundi",
        "BJ|Бенин|Benin",
        "BM|Бермуды|Bermuda",
        "BN|Бруней|Brunei Darussalam",
        "BO|Боливия|Bolivia",
        "BQ|Бонайре, Синт-Эстатиус и Саба|Bonaire, Sint Eustatius & Saba",
        "BR|Бразилия|Brazil",
        "BS|Багамы|Bahamas",
        "BT|Бутан|Bhutan",
        "BW|Ботсвана|Botswana",
        "BY|Беларусь|Belarus",
        "BZ|Белиз|Belize",
        "CA|Канада|Canada",
        "CD|Демократическая Республика Конго|Congo (Dem. Rep.)",
        "CF|Центральноафриканская Республика|Central African Rep.",
        "CG|Республика Конго|Congo (Rep.)",
        "CH|Швейцария|Switzerland",
        "CI|Кот-д'Ивуар|Côte d`Ivoire",
        "CK|Острова Кука|Cook Islands",
        "CL|Чили|Chile",
        "CM|Камерун|Cameroon",
        "CN|Китай|China",
        "CO|Колумбия|Colombia",
        "CR|Коста-Рика|Costa Rica",
        "CU|Куба|Cuba",
        "CV|Кабо-Верде|Cape Verde",
        "CW|Кюрасао|Curaçao",
        "CY|Кипр|Cyprus",
        "CZ|Чехия|Czech Republic",
    };

    private static final String[] COUNTRY_DATA_1 = new String[] {
        "DE|Германия|Germany",
        "DJ|Джибути|Djibouti",
        "DK|Дания|Denmark",
        "DM|Доминика|Dominica",
        "DO|Доминиканская Республика|Dominican Rep.",
        "DZ|Алжир|Algeria",
        "EC|Эквадор|Ecuador",
        "EE|Эстония|Estonia",
        "EG|Египет|Egypt",
        "ER|Эритрея|Eritrea",
        "ES|Испания|Spain",
        "ET|Эфиопия|Ethiopia",
        "FI|Финляндия|Finland",
        "FJ|Фиджи|Fiji",
        "FK|Фолклендские острова|Falkland Islands",
        "FM|Микронезия|Micronesia",
        "FO|Фарерские острова|Faroe Islands",
        "FR|Франция|France",
        "GA|Габон|Gabon",
        "GB|Великобритания|United Kingdom",
        "GD|Гренада|Grenada",
        "GE|Грузия|Georgia",
        "GF|Французская Гвиана|French Guiana",
        "GH|Гана|Ghana",
        "GI|Гибралтар|Gibraltar",
        "GL|Гренландия|Greenland",
        "GM|Гамбия|Gambia",
        "GN|Гвинея|Guinea",
        "GP|Гваделупа|Guadeloupe",
        "GQ|Экваториальная Гвинея|Equatorial Guinea",
        "GR|Греция|Greece",
        "GT|Гватемала|Guatemala",
        "GU|Гуам|Guam",
        "GW|Гвинея-Бисау|Guinea-Bissau",
        "GY|Гайана|Guyana",
        "HK|Гонконг (Китай)|Hong Kong (China)",
        "HN|Гондурас|Honduras",
        "HR|Хорватия|Croatia",
        "HT|Гаити|Haiti",
        "HU|Венгрия|Hungary",
        "ID|Индонезия|Indonesia",
        "IE|Ирландия|Ireland",
        "IL|Израиль|Israel",
        "IN|Индия|India",
        "IO|Диего-Гарсия|Diego Garcia",
        "IQ|Ирак|Iraq",
        "IR|Иран|Iran",
        "IS|Исландия|Iceland",
        "IT|Италия|Italy",
        "JM|Ямайка|Jamaica",
    };

    private static final String[] COUNTRY_DATA_2 = new String[] {
        "JO|Иордания|Jordan",
        "JP|Япония|Japan",
        "KE|Кения|Kenya",
        "KG|Кыргызстан|Kyrgyzstan",
        "KH|Камбоджа|Cambodia",
        "KI|Кирибати|Kiribati",
        "KM|Коморы|Comoros",
        "KN|Сент-Китс и Невис|Saint Kitts & Nevis",
        "KP|Северная Корея|North Korea",
        "KR|Южная Корея|South Korea",
        "KW|Кувейт|Kuwait",
        "KY|Каймановы острова|Cayman Islands",
        "KZ|Казахстан|Kazakhstan",
        "LA|Лаос|Laos",
        "LB|Ливан|Lebanon",
        "LC|Сент-Люсия|Saint Lucia",
        "LI|Лихтенштейн|Liechtenstein",
        "LK|Шри-Ланка|Sri Lanka",
        "LR|Либерия|Liberia",
        "LS|Лесото|Lesotho",
        "LT|Литва|Lithuania",
        "LU|Люксембург|Luxembourg",
        "LV|Латвия|Latvia",
        "LY|Ливия|Libya",
        "MA|Марокко|Morocco",
        "MC|Монако|Monaco",
        "MD|Молдова|Moldova",
        "ME|Черногория|Montenegro",
        "MG|Мадагаскар|Madagascar",
        "MH|Маршалловы острова|Marshall Islands",
        "MK|Македония|Macedonia",
        "ML|Мали|Mali",
        "MM|Мьянма|Myanmar",
        "MN|Монголия|Mongolia",
        "MO|Макао|Macau",
        "MP|Северные Марианские острова|Northern Mariana Islands",
        "MQ|Мартиника|Martinique",
        "MR|Мавритания|Mauritania",
        "MS|Монтсеррат|Montserrat",
        "MT|Мальта|Malta",
        "MU|Маврикий|Mauritius",
        "MV|Мальдивы|Maldives",
        "MW|Малави|Malawi",
        "MX|Мексика|Mexico",
        "MY|Малайзия|Malaysia",
        "MZ|Мозамбик|Mozambique",
        "NA|Намибия|Namibia",
        "NC|Новая Каледония|New Caledonia",
        "NE|Нигер|Niger",
        "NF|Остров Норфолк|Norfolk Island",
    };

    private static final String[] COUNTRY_DATA_3 = new String[] {
        "NG|Нигерия|Nigeria",
        "NI|Никарагуа|Nicaragua",
        "NL|Нидерланды|Netherlands",
        "NN|Нарния|Narnia",
        "NO|Норвегия|Norway",
        "NP|Непал|Nepal",
        "NR|Науру|Nauru",
        "NU|Ниуэ|Niue",
        "NZ|Новая Зеландия|New Zealand",
        "OM|Оман|Oman",
        "PA|Панама|Panama",
        "PE|Перу|Peru",
        "PF|Французская Полинезия|French Polynesia",
        "PG|Папуа-Новая Гвинея|Papua New Guinea",
        "PH|Филиппины|Philippines",
        "PK|Пакистан|Pakistan",
        "PL|Польша|Poland",
        "PM|Сен-Пьер и Микелон|Saint Pierre & Miquelon",
        "PR|Пуэрто-Рико|Puerto Rico",
        "PS|Палестина|Palestine",
        "PT|Португалия|Portugal",
        "PW|Палау|Palau",
        "PY|Парагвай|Paraguay",
        "QA|Катар|Qatar",
        "RE|Реюньон|Réunion",
        "RO|Румыния|Romania",
        "RS|Сербия|Serbia",
        "RU|Россия|Russia",
        "RW|Руанда|Rwanda",
        "SA|Саудовская Аравия|Saudi Arabia",
        "SB|Соломоновы острова|Solomon Islands",
        "SC|Сейшелы|Seychelles",
        "SD|Судан|Sudan",
        "SE|Швеция|Sweden",
        "SG|Сингапур|Singapore",
        "SH|Остров Святой Елены|Saint Helena",
        "SH|Остров Святой Елены|Saint Helena",
        "SI|Словения|Slovenia",
        "SK|Словакия|Slovakia",
        "SL|Сьерра-Леоне|Sierra Leone",
        "SM|Сан-Марино|San Marino",
        "SN|Сенегал|Senegal",
        "SO|Сомали|Somalia",
        "SR|Суринам|Suriname",
        "SS|Южный Судан|South Sudan",
        "ST|Сан-Томе и Принсипи|São Tomé & Príncipe",
        "SV|Сальвадор|El Salvador",
        "SX|Синт-Мартен|Sint Maarten",
        "SY|Сирийская Арабская Республика|Syrian Arab Republic",
        "SZ|Свазиленд|Swaziland",
    };

    private static final String[] COUNTRY_DATA_4 = new String[] {
        "TC|Теркс и Кайкос|Turks & Caicos Islands",
        "TD|Чад|Chad",
        "TG|Того|Togo",
        "TH|Таиланд|Thailand",
        "TJ|Таджикистан|Tajikistan",
        "TK|Токелау|Tokelau",
        "TL|Восточный Тимор|Timor-Leste",
        "TM|Туркменистан|Turkmenistan",
        "TN|Тунис|Tunisia",
        "TO|Тонга|Tonga",
        "TR|Турция|Turkey",
        "TT|Тринидад и Тобаго|Trinidad & Tobago",
        "TV|Тувалу|Tuvalu",
        "TW|Тайвань (Китай)|Taiwan (China)",
        "TZ|Танзания|Tanzania",
        "UA|Украина|Ukraine",
        "UG|Уганда|Uganda",
        "US|США|USA",
        "UY|Уругвай|Uruguay",
        "UZ|Узбекистан|Uzbekistan",
        "VC|Сент-Винсент и Гренадины|Saint Vincent & the Grenadines",
        "VE|Венесуэла|Venezuela",
        "VG|Британские Виргинские острова|British Virgin Islands",
        "VI|Американские Виргинские острова|US Virgin Islands",
        "VN|Вьетнам|Vietnam",
        "VU|Вануату|Vanuatu",
        "WF|Уоллис и Футуна|Wallis & Futuna",
        "WS|Самоа|Samoa",
        "YE|Йемен|Yemen",
        "ZA|Южная Африка|South Africa",
        "ZM|Замбия|Zambia",
        "ZW|Зимбабве|Zimbabwe",
        "YT|Майотта|Mayotte",
        "KM|Коморские острова|Comoros",
        "IO|Британская территория в Индийском океане|British Indian Ocean Territory",
        "SC|Сейшельские острова|Seychelles",
        "EH|Западная Сахара|Western Sahara",
        "TF|Французские южные территории|French Southern Territories",
        "SZ|Эсватини|Eswatini",
        "SH|Святая Елена|Saint Helena",
        "VG|Виргинские острова (Британские)|British Virgin Islands",
        "UM|Малые отдаленные острова Соединенных Штатов|United States Minor Outlying Islands",
        "US|Соединенные Штаты|United States",
        "CC|Кокосовые (Килинг) острова|Cocos (Keeling) Islands",
        "CX|Остров Рождества|Christmas Island",
        "PN|Остров Питкэрн|Pitcairn",
        "AQ|Антарктида|Antarctica",
        "BV|Остров Буве|Bouvet Island",
        "HM|Остров Херд и острова Макдональд|Heard Island and McDonald Islands",
        "XK|Косово|Kosovo",
    };

    private static final String[] COUNTRY_DATA_5 = new String[] {
        "VA|Город-государство Ватикан (Святой Престол)|Vatican City",
        "CZ|Чешская Республика|Czech Republic",
        "PS|Оккупированная палестинская территория|Palestinian Territory",
        "SJ|Шпицберген и острова Ян-Майен|Svalbard and Jan Mayen",
        "AX|Аландские острова|Aland Islands",
        "GG|Гернси и Олдерни|Guernsey",
        "JE|Джерси|Jersey",
        "IM|Мэн (остров)|Isle of Man",
        "PM|Сен-Пьер и Микелон|Saint Pierre and Miquelon",
        "SS|Южный Судан|South Sudan",
        "MK|Северная Македония|North Macedonia",
        "TL|Тимор-Лешти|Timor-Leste",
        "SY|Сирия|Syria",
        "GE|Джорджия|Georgia",
        "JO|Джордан|Jordan",
        "RE|Воссоединение|Reunion",
        "US|США|USA",
        "RU|РФ|Russian Federation",
    };


    private static void ensureDictionaries() {
        if (!NAME_TO_CODE.isEmpty()) return;
        String[][] allChunks = new String[][] { COUNTRY_DATA_0, COUNTRY_DATA_1, COUNTRY_DATA_2, COUNTRY_DATA_3, COUNTRY_DATA_4, COUNTRY_DATA_5 };
        for (String[] chunk : allChunks) {
            for (String entry : chunk) {
                if (entry == null || entry.isEmpty()) continue;
                String[] p = entry.split("\\|");
                if (p.length >= 2) {
                    String code = p[0].trim().toUpperCase(Locale.ROOT);
                    String rus = p[1].trim();
                    String en = p.length >= 3 ? p[2].trim() : "";

                    if (!code.isEmpty()) {
                        if (!rus.isEmpty()) {
                            NAME_TO_CODE.put(normalize(rus), code);
                            if (!CODE_TO_NAME.containsKey(code)) {
                                CODE_TO_NAME.put(code, rus);
                            }
                        }
                        if (!en.isEmpty()) {
                            NAME_TO_CODE.put(normalize(en), code);
                        }
                    }
                }
            }
        }
    }

    private static final Set<String> visitedCodes = new HashSet<String>();
    private static final Set<String> visitedNames = new HashSet<String>();
    private static final Map<String, VisitedCountry> visitedMap = new LinkedHashMap<String, VisitedCountry>();
    private static final List<Runnable> listeners = new ArrayList<Runnable>();
    private static volatile long lastSyncTime = 0L;
    private static volatile String lastSyncSource = "";
    private static volatile boolean initialized = false;

    private VisitedCountriesManager() {}

    public static synchronized void init(Context context) {
        if (initialized && !visitedCodes.isEmpty()) return;
        ensureDictionaries();
        if (context == null) return;

        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        lastSyncTime = p.getLong("visited_countries_last_sync", 0L);
        lastSyncSource = p.getString("visited_countries_source", "Кэш");

        String json = p.getString("visited_countries_json", null);
        if (json != null && !json.trim().isEmpty()) {
            loadFromSavedJson(json);
        }

        if (visitedCodes.isEmpty()) {
            Set<String> savedCodes = p.getStringSet("visited_countries_codes", null);
            if (savedCodes != null && !savedCodes.isEmpty()) {
                for (String c : savedCodes) {
                    c = c.trim().toUpperCase(Locale.ROOT);
                    if (!c.isEmpty()) {
                        visitedCodes.add(c);
                        String name = CODE_TO_NAME.get(c);
                        if (name != null) visitedNames.add(normalize(name));
                    }
                }
            }
        }

        if (visitedCodes.isEmpty()) {
            scanCacheDirectory(context);
        }

        initialized = true;
    }

    private static synchronized void loadFromSavedJson(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                VisitedCountry vc = VisitedCountry.fromJSON(obj);
                if (vc != null) {
                    if (!vc.code.isEmpty()) visitedCodes.add(vc.code);
                    if (!vc.name.isEmpty()) visitedNames.add(normalize(vc.name));
                    visitedMap.put(vc.code.isEmpty() ? vc.name : vc.code, vc);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static String getFlagForCode(String code) {
        if (code == null || code.length() != 2) return "🌐";
        code = code.toUpperCase(Locale.ROOT);
        char c1 = code.charAt(0);
        char c2 = code.charAt(1);
        if (c1 < 'A' || c1 > 'Z' || c2 < 'A' || c2 > 'Z') return "🌐";
        int cp1 = 0x1F1E6 + (c1 - 'A');
        int cp2 = 0x1F1E6 + (c2 - 'A');
        return new String(Character.toChars(cp1)) + new String(Character.toChars(cp2));
    }

    public static String getFlagForCountry(String rawName) {
        ensureDictionaries();
        String code = resolveCountryCode(rawName);
        if (code != null && code.length() == 2) {
            return getFlagForCode(code);
        }
        return "🌐";
    }

    public static String extractCountryName(String lineName) {
        if (lineName == null) return "";
        int first = lineName.indexOf('(');
        int last = lineName.lastIndexOf(')');
        if (first >= 0 && last > first) {
            return lineName.substring(first + 1, last).trim();
        }
        return lineName.trim();
    }

    public static String resolveCountryCode(String rawName) {
        ensureDictionaries();
        if (rawName == null || rawName.trim().isEmpty()) return null;
        String country = extractCountryName(rawName);
        String norm = normalize(country);

        String direct = NAME_TO_CODE.get(norm);
        if (direct != null) return direct;

        if (country.length() == 2) {
            String upper = country.toUpperCase(Locale.ROOT);
            if (CODE_TO_NAME.containsKey(upper) || (upper.charAt(0) >= 'A' && upper.charAt(0) <= 'Z')) {
                return upper;
            }
        }

        if (norm.contains("сша") || norm.contains("usa") || norm.contains("united states")) {
            return "US";
        }
        if (norm.contains("россия") || norm.contains("рф") || norm.contains("russia")) {
            return "RU";
        }

        for (Map.Entry<String, String> e : NAME_TO_CODE.entrySet()) {
            String key = e.getKey();
            if (key.length() >= 4 && (norm.contains(key) || key.contains(norm))) {
                return e.getValue();
            }
        }

        return null;
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .replace('-', ' ')
            .replace(',', ' ')
            .replace('.', ' ')
            .replace('(', ' ')
            .replace(')', ' ');
        return lower.replaceAll("\\s+", " ").trim();
    }

    public static synchronized boolean isCountryVisited(String countryName) {
        ensureDictionaries();
        if (countryName == null || countryName.trim().isEmpty()) return false;
        String code = resolveCountryCode(countryName);
        if (code != null && visitedCodes.contains(code)) {
            return true;
        }

        String norm = normalize(countryName);
        if (visitedNames.contains(norm)) {
            return true;
        }

        if (norm.contains("сша") && visitedCodes.contains("US")) {
            return true;
        }

        for (String vn : visitedNames) {
            if (vn.length() >= 4 && (norm.contains(vn) || vn.contains(norm))) {
                return true;
            }
        }

        return false;
    }

    public static synchronized void markCountryVisited(String countryName) {
        ensureDictionaries();
        if (countryName == null || countryName.trim().isEmpty()) return;
        String code = resolveCountryCode(countryName);
        if (code != null) visitedCodes.add(code);
        String norm = normalize(countryName);
        visitedNames.add(norm);

        Context c = StepHooks.getApp();
        if (c != null) {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit()
                .putStringSet("visited_countries_codes", new HashSet<String>(visitedCodes))
                .apply();
        }
        notifyListeners();
    }

    public static synchronized boolean parseAndSaveFootprints(Context context, String rawJson, String source) {
        if (rawJson == null || rawJson.trim().isEmpty()) return false;
        ensureDictionaries();

        boolean anyAdded = false;
        try {
            String trimmed = rawJson.trim();
            if (trimmed.startsWith("{")) {
                JSONObject root = new JSONObject(trimmed);
                if (root.has("countries")) {
                    Object countriesObj = root.get("countries");
                    if (countriesObj instanceof JSONArray) {
                        anyAdded |= parseCountriesArray((JSONArray) countriesObj);
                    } else if (countriesObj instanceof JSONObject) {
                        anyAdded |= parseCountriesObject((JSONObject) countriesObj);
                    }
                } else if (root.has("id") || root.has("country_code")) {
                    anyAdded |= parseSingleCountryObject(root);
                } else {
                    anyAdded |= parseCountriesObject(root);
                }
            } else if (trimmed.startsWith("[")) {
                JSONArray rootArr = new JSONArray(trimmed);
                anyAdded |= parseCountriesArray(rootArr);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error parsing footprints JSON from " + source, e);
            return false;
        }

        if (anyAdded) {
            lastSyncTime = System.currentTimeMillis();
            lastSyncSource = source == null ? "Сеть" : source;

            if (context != null) {
                try {
                    JSONArray arr = new JSONArray();
                    for (VisitedCountry vc : visitedMap.values()) {
                        arr.put(vc.toJSON());
                    }
                    SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                    p.edit()
                        .putString("visited_countries_json", arr.toString())
                        .putStringSet("visited_countries_codes", new HashSet<String>(visitedCodes))
                        .putLong("visited_countries_last_sync", lastSyncTime)
                        .putString("visited_countries_source", lastSyncSource)
                        .apply();
                } catch (Throwable ignored) {}
            }

            StepHooks.log("[Footprints] " + lastSyncSource + ": синхронизировано " + visitedCodes.size() + " стран");
            notifyListeners();
        }

        return anyAdded;
    }

    private static boolean parseCountriesArray(JSONArray arr) {
        boolean added = false;
        for (int i = 0; i < arr.length(); i++) {
            try {
                Object item = arr.get(i);
                if (item instanceof JSONObject) {
                    added |= parseSingleCountryObject((JSONObject) item);
                } else if (item instanceof String) {
                    String str = ((String) item).trim();
                    if (!str.isEmpty()) {
                        String code = resolveCountryCode(str);
                        if (code == null && str.length() == 2) code = str.toUpperCase(Locale.ROOT);
                        String name = code != null ? CODE_TO_NAME.get(code) : str;
                        if (name == null) name = str;
                        VisitedCountry vc = new VisitedCountry(code, name, getFlagForCode(code), 1, System.currentTimeMillis());
                        recordVisited(vc);
                        added = true;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return added;
    }

    private static boolean parseCountriesObject(JSONObject obj) {
        boolean added = false;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key == null || key.isEmpty()) continue;
            try {
                Object val = obj.get(key);
                if (val instanceof JSONObject) {
                    JSONObject sub = (JSONObject) val;
                    String code = sub.optString("id", sub.optString("code", key));
                    String name = sub.optString("name", CODE_TO_NAME.get(code.toUpperCase(Locale.ROOT)));
                    String flag = sub.optString("flag", "");
                    int cities = sub.optInt("cities_count", sub.optInt("citiesVisited", 1));
                    VisitedCountry vc = new VisitedCountry(code, name, flag, cities, System.currentTimeMillis());
                    recordVisited(vc);
                    added = true;
                } else {
                    String code = resolveCountryCode(key);
                    if (code == null && key.length() == 2) code = key.toUpperCase(Locale.ROOT);
                    String name = code != null ? CODE_TO_NAME.get(code) : key;
                    if (name == null) name = key;
                    int cities = 1;
                    if (val instanceof Number) cities = ((Number) val).intValue();
                    VisitedCountry vc = new VisitedCountry(code, name, getFlagForCode(code), cities, System.currentTimeMillis());
                    recordVisited(vc);
                    added = true;
                }
            } catch (Throwable ignored) {}
        }
        return added;
    }

    private static boolean parseSingleCountryObject(JSONObject item) {
        String code = item.optString("id", item.optString("code", item.optString("country_code", "")));
        String name = item.optString("name", item.optString("title", item.optString("country", "")));
        String flag = item.optString("flag", "");
        int cities = item.optInt("cities_count", item.optInt("citiesVisited", 1));

        if (code.isEmpty() && !name.isEmpty()) {
            code = resolveCountryCode(name);
        }
        if (name.isEmpty() && !code.isEmpty()) {
            name = CODE_TO_NAME.get(code.toUpperCase(Locale.ROOT));
            if (name == null) name = code;
        }

        if (!code.isEmpty() || !name.isEmpty()) {
            VisitedCountry vc = new VisitedCountry(code, name, flag, cities, System.currentTimeMillis());
            recordVisited(vc);
            return true;
        }
        return false;
    }

    private static void recordVisited(VisitedCountry vc) {
        if (vc == null) return;
        if (!vc.code.isEmpty()) visitedCodes.add(vc.code);
        if (!vc.name.isEmpty()) visitedNames.add(normalize(vc.name));
        String key = !vc.code.isEmpty() ? vc.code : vc.name;
        visitedMap.put(key, vc);
    }

    public static void onHttpResponse(String url, String body) {
        if (url == null || body == null) return;
        if (url.contains("/api/footprints/countries") ||
            url.contains("/api/footprints/stats/world") ||
            url.contains("/api/footprints/stats/country") ||
            url.contains("/api/footprints/v2/android")) {
            Context c = StepHooks.getApp();
            parseAndSaveFootprints(c, body, "OkHttp: " + (url.contains("stats/world") ? "stats/world" : "footprints"));
        }
    }

    public static void onHttpHeader(String name, String value) {
        if (name == null || value == null) return;
        if ("authorization".equalsIgnoreCase(name.trim())) {
            saveAuthToken(value.trim());
        }
    }

    public static void saveAuthToken(String token) {
        if (token == null || token.isEmpty()) return;
        Context c = StepHooks.getApp();
        if (c != null) {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("auth_token", token)
                .apply();
        }
    }

    public static String getAuthToken(Context context) {
        if (context == null) context = StepHooks.getApp();
        if (context == null) return null;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("auth_token", null);
    }

    public static boolean scanCacheDirectory(Context context) {
        if (context == null) context = StepHooks.getApp();
        if (context == null) return false;

        try {
            File cacheDir = new File(context.getCacheDir(), "dir_world_footprints");
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                File[] subs = cacheDir.listFiles();
                if (subs != null) {
                    for (File f : subs) {
                        File target = f.isDirectory() ? new File(f, "world") : (f.getName().equals("world") ? f : null);
                        if (target != null && target.exists() && target.isFile() && target.length() > 0) {
                            String json = readFileToString(target);
                            if (json != null && !json.trim().isEmpty()) {
                                boolean ok = parseAndSaveFootprints(context, json, "Кэш dir_world_footprints");
                                if (ok) return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error scanning cache dir", e);
        }
        return false;
    }

    private static String readFileToString(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
            return sb.toString();
        } catch (Throwable e) {
            return null;
        }
    }

    public static void syncFromServerAsync(final Context context, final SyncCallback callback) {
        final Context ctx = context != null ? context : StepHooks.getApp();
        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                String token = getAuthToken(ctx);
                boolean success = false;
                String errorMsg = null;

                if (token != null && !token.isEmpty()) {
                    try {
                        String[] endpoints = new String[] {
                            "https://api.blink-maps.com/api/footprints/stats/world",
                            "https://api.blink-maps.com/api/footprints/countries"
                        };

                        for (String ep : endpoints) {
                            HttpURLConnection conn = null;
                            try {
                                URL u = new URL(ep);
                                conn = (HttpURLConnection) u.openConnection();
                                conn.setRequestMethod("GET");
                                conn.setConnectTimeout(10000);
                                conn.setReadTimeout(15000);
                                conn.setRequestProperty("Authorization", token);
                                conn.setRequestProperty("Accept", "application/json");
                                conn.setRequestProperty("User-Agent", "Blink/1.32.0 (Android)");

                                int code = conn.getResponseCode();
                                if (code >= 200 && code < 300) {
                                    InputStream is = conn.getInputStream();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                                    StringBuilder sb = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        sb.append(line);
                                    }
                                    reader.close();
                                    success = parseAndSaveFootprints(ctx, sb.toString(), "Прямой API (" + (ep.contains("stats") ? "stats/world" : "countries") + ")");
                                    if (success) break;
                                } else {
                                    errorMsg = "HTTP " + code + " (" + ep + ")";
                                }
                            } finally {
                                if (conn != null) conn.disconnect();
                            }
                        }
                    } catch (Throwable e) {
                        errorMsg = "Сетевая ошибка: " + e.getMessage();
                        Log.e(TAG, "Sync from server failed", e);
                    }
                }

                if (!success) {
                    boolean cacheOk = scanCacheDirectory(ctx);
                    if (cacheOk) {
                        success = true;
                    } else if (errorMsg == null) {
                        errorMsg = "Токен авторизации ещё не сохранён. Откройте карту или профиль в Blink.";
                    }
                }

                final boolean finalSuccess = success;
                final String finalError = errorMsg;
                final CountryStats stats = getStats(ctx);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            if (finalSuccess) {
                                callback.onSuccess(stats);
                            } else {
                                callback.onError(finalError != null ? finalError : "Не удалось получить данные о странах");
                            }
                        }
                    }
                });
            }
        }, "BlinkMod-FootprintsSync").start();
    }

    public static synchronized List<WorldTour.City> getMissingCountries(Context context) {
        init(context);
        WorldTour.City[] allTargets = WorldTour.loadFromAsset(context, "world_countries.dat", WorldTour.WORLD_CAPITALS);
        ArrayList<WorldTour.City> missing = new ArrayList<WorldTour.City>();
        Set<String> seenCountries = new HashSet<String>();

        for (WorldTour.City city : allTargets) {
            String country = extractCountryName(city.name);
            String norm = normalize(country);

            if (seenCountries.contains(norm)) continue;
            seenCountries.add(norm);

            if (!isCountryVisited(country)) {
                missing.add(city);
            }
        }

        return missing;
    }

    public static synchronized CountryStats getStats(Context context) {
        init(context);
        WorldTour.City[] allTargets = WorldTour.loadFromAsset(context, "world_countries.dat", WorldTour.WORLD_CAPITALS);
        Set<String> uniqueCountries = new HashSet<String>();
        for (WorldTour.City c : allTargets) {
            uniqueCountries.add(normalize(extractCountryName(c.name)));
        }

        int total = uniqueCountries.size();
        List<WorldTour.City> missing = getMissingCountries(context);
        int missingCount = missing.size();
        int visited = Math.max(0, total - missingCount);

        return new CountryStats(total, visited, missingCount, lastSyncTime, lastSyncSource);
    }

    public static synchronized List<VisitedCountry> getVisitedList() {
        return new ArrayList<VisitedCountry>(visitedMap.values());
    }

    public static void addListener(Runnable r) {
        if (r == null) return;
        synchronized (listeners) {
            if (!listeners.contains(r)) listeners.add(r);
        }
    }

    public static void removeListener(Runnable r) {
        if (r == null) return;
        synchronized (listeners) {
            listeners.remove(r);
        }
    }

    private static void notifyListeners() {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                List<Runnable> copy;
                synchronized (listeners) {
                    copy = new ArrayList<Runnable>(listeners);
                }
                for (Runnable r : copy) {
                    try {
                        r.run();
                    } catch (Throwable ignored) {}
                }
            }
        });
    }
}
