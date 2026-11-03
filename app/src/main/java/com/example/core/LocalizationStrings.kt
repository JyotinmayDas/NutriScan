package com.example.core

data class AppStrings(
    val appTitle: String,
    val tagline: String,
    val navHome: String,
    val navScan: String,
    val navDiary: String,
    val navCoach: String,
    val navProfile: String,
    val caloriesRemaining: String,
    val caloriesConsumed: String,
    val dailyTarget: String,
    val protein: String,
    val carbs: String,
    val fat: String,
    val fiber: String,
    val sugar: String,
    val waterIntake: String,
    val weightTracker: String,
    val quickScanMeal: String,
    val quickAddFood: String,
    val aiCoachBanner: String,
    val medicalDisclaimer: String
)

object LocalizationCatalog {
    fun forLanguage(lang: String): AppStrings = when (lang) {
        "bn" -> AppStrings(
            appTitle = "NutriScan • নিউট্রি-স্ক্যান",
            tagline = "খাবার স্ক্যান করুন। পুষ্টি জানুন।",
            navHome = "হোম",
            navScan = "স্ক্যান",
            navDiary = "ডায়েরি",
            navCoach = "প্রগতি",
            navProfile = "প্রোফাইল",
            caloriesRemaining = "বাকি ক্যালোরি",
            caloriesConsumed = "গ্রহণ করা হয়েছে",
            dailyTarget = "দৈনিক লক্ষ্য",
            protein = "প্রোটিন",
            carbs = "কার্বোহাইড্রেট",
            fat = "ফ্যাট",
            fiber = "ফাইবার",
            sugar = "চিনি",
            waterIntake = "জল পান (হাইড্রেশন)",
            weightTracker = "ওজন ও রিপোর্ট",
            quickScanMeal = "খাবারের ছবি স্ক্যান করুন",
            quickAddFood = "খাবার যোগ করুন",
            aiCoachBanner = "বাংলা, হিন্দি ও ইংরেজিতে ব্যক্তিগত খাবার ও পুষ্টি পরিকল্পনা",
            medicalDisclaimer = "এই হিসাবগুলি আনুমানিক ধারণা মাত্র, কোনো ডাক্তারি পরামর্শ নয়।"
        )
        "hi" -> AppStrings(
            appTitle = "NutriScan • न्यूट्री-स्कैन",
            tagline = "भोजन स्कैन करें। अपना पोषण जानें।",
            navHome = "होम",
            navScan = "स्कैन",
            navDiary = "डायरी",
            navCoach = "प्रगति",
            navProfile = "प्रोफ़ाइल",
            caloriesRemaining = "शेष कैलोरी",
            caloriesConsumed = "ली गई कैलोरी",
            dailyTarget = "दैनिक लक्ष्य",
            protein = "प्रोटीन",
            carbs = "कार्ब्स",
            fat = "वसा (Fat)",
            fiber = "फाइबर",
            sugar = "चीनी",
            waterIntake = "जल सेवन (Hydration)",
            weightTracker = "वज़न और रिपोर्ट",
            quickScanMeal = "भोजन फोटो स्कैन करें",
            quickAddFood = "भोजन जोड़ें",
            aiCoachBanner = "हिंदी, बंगाली और अंग्रेज़ी में व्यक्तिगत आहार और प्रगति ट्रैकिंग",
            medicalDisclaimer = "ये गणनाएँ केवल अनुमान हैं, चिकित्सा सलाह नहीं हैं।"
        )
        else -> AppStrings(
            appTitle = "NutriScan",
            tagline = "Scan Your Food. Know Your Nutrition.",
            navHome = "Home",
            navScan = "Scan",
            navDiary = "Diary",
            navCoach = "Progress",
            navProfile = "Profile",
            caloriesRemaining = "Remaining kcal",
            caloriesConsumed = "Consumed",
            dailyTarget = "Daily Goal",
            protein = "Protein",
            carbs = "Carbs",
            fat = "Fat",
            fiber = "Fiber",
            sugar = "Sugar",
            waterIntake = "Water Intake",
            weightTracker = "Weight & Reports",
            quickScanMeal = "Scan Meal",
            quickAddFood = "Quick Add Food",
            aiCoachBanner = "Personalized Meal Planning & Progress in English, বাংলা & हिन्दी",
            medicalDisclaimer = "Nutritional calculations and visual analyses are estimates, not medical advice."
        )
    }
}
