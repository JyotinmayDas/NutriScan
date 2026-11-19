package com.example.data

import com.example.model.FoodDatabaseItem

object FoodDatabaseCatalog {
    val categories = listOf(
        "All",
        "Indian Foods",
        "South Indian Foods",
        "Nepali Foods",
        "Bengali Foods",
        "Rice and Grains",
        "Meat and Fish",
        "Vegetables",
        "Dairy Products",
        "Fruits",
        "Snacks",
        "Beverages",
        "Fast Food",
        "International Foods"
    )

    val allFoods: List<FoodDatabaseItem> = listOf(
        // ==================== BENGALI TRADITIONAL FOODS ====================
        FoodDatabaseItem(
            id = "bn_macher_jhol",
            nameEn = "Rohu Fish Curry (Macher Jhol)",
            nameBn = "রুই মাছের ঝোল",
            nameHi = "रोहू मछली करी",
            category = "Bengali Foods",
            servingSizeG = 180.0,
            servingLabel = "1 bowl (1 piece fish + gravy)",
            calories = 235,
            protein = 22.4,
            carbohydrates = 7.8,
            fat = 12.5,
            fiber = 1.6,
            sugar = 1.8,
            cookingMethod = "Simmered in light cumin, ginger, and mustard oil gravy",
            cuisine = "Bengali",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "120g Rohu fish steak, turmeric & salt rubbed",
                "1 medium potato, cut into wedges",
                "1 tsp ginger-cumin paste, 1/2 tsp nigella seeds (kalonji)",
                "1 tbsp mustard oil, 1 green chili, 1 cup warm water"
            ),
            recipeSteps = listOf(
                "Lightly pan-fry turmeric-marinated Rohu fish in 1 tbsp mustard oil for 2 minutes per side and set aside.",
                "Temper kalonji and slit green chili in the same oil, then sauté potato wedges with ginger-cumin paste and turmeric.",
                "Pour 1 cup warm water, cover for 8 minutes until potatoes soften, slide in fried fish, simmer 4 minutes, and serve hot."
            )
        ),
        FoodDatabaseItem(
            id = "bn_shorshe_ilish",
            nameEn = "Shorshe Ilish (Hilsa in Mustard)",
            nameBn = "সর্ষে ইলিশ",
            nameHi = "सरसों इलिश",
            category = "Bengali Foods",
            servingSizeG = 150.0,
            servingLabel = "1 piece with mustard gravy",
            calories = 340,
            protein = 21.8,
            carbohydrates = 4.2,
            fat = 26.0,
            fiber = 1.2,
            sugar = 0.8,
            cookingMethod = "Steamed/simmered in ground mustard paste and mustard oil",
            cuisine = "Bengali",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "120g Hilsa (Ilish) steak",
                "2 tbsp black & yellow mustard paste with 2 green chilies",
                "1/2 tsp turmeric powder, 1/2 tsp kalonji",
                "1.5 tbsp raw mustard oil & salt to taste"
            ),
            recipeSteps = listOf(
                "Marinate Hilsa gently with salt and turmeric for 10 minutes.",
                "Temper kalonji and green chilies in mustard oil, then add strained mustard paste with 1/2 cup warm water.",
                "Slide fish pieces into simmering gravy, cover on low flame for 7–8 minutes, and drizzle raw mustard oil before serving."
            )
        ),
        FoodDatabaseItem(
            id = "bn_shukto",
            nameEn = "Bengali Shukto (Mixed Veg Stew)",
            nameBn = "শুক্তো",
            nameHi = "बंगाली शुक्तो",
            category = "Bengali Foods",
            servingSizeG = 150.0,
            servingLabel = "1 medium katori",
            calories = 145,
            protein = 3.8,
            carbohydrates = 16.5,
            fat = 7.2,
            fiber = 4.4,
            sugar = 3.1,
            cookingMethod = "Slow-cooked bitter gourd, green banana, drumstick, and poppy seed paste",
            cuisine = "Bengali",
            prepTimeMinutes = 30,
            ingredients = listOf(
                "40g bitter gourd (korola), 50g raw banana & potato batons",
                "40g drumsticks & sweet potato, 1 tbsp poppy seed-mustard paste",
                "1/2 cup toned milk, 1 tsp panch phoron & radhuni",
                "1 tsp ghee & 1 tsp mustard oil"
            ),
            recipeSteps = listOf(
                "Lightly fry bitter gourd slices until crisp and set aside.",
                "Temper panch phoron and bay leaf, sauté remaining vegetables with ginger paste, then add milk and water to simmer tender.",
                "Stir in poppy seed-mustard paste and fried bitter gourd, cook 3 minutes, and finish with 1 tsp ghee."
            )
        ),
        FoodDatabaseItem(
            id = "bn_cholar_dal",
            nameEn = "Bengali Cholar Dal",
            nameBn = "ছোলার ডাল",
            nameHi = "बंगाली चना दाल",
            category = "Bengali Foods",
            servingSizeG = 150.0,
            servingLabel = "1 bowl (150g)",
            calories = 210,
            protein = 9.5,
            carbohydrates = 28.0,
            fat = 6.8,
            fiber = 5.6,
            sugar = 4.2,
            cookingMethod = "Bengal gram tempered with ghee, coconut bits, and whole spices",
            cuisine = "Bengali",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "60g split Bengal gram (chana dal), soaked 30 mins",
                "1 tbsp sliced fresh coconut, 1 tsp raisins",
                "1 tsp grated ginger, 1/2 tsp cumin, bay leaf & cinnamon",
                "1 tsp ghee & pinch of jaggery"
            ),
            recipeSteps = listOf(
                "Pressure-cook soaked chana dal with turmeric and salt until grains are tender yet whole.",
                "Fry coconut slices in 1 tsp ghee until golden, then temper cumin, bay leaf, whole spices, and ginger.",
                "Pour cooked dal into the tempering, simmer 5 minutes with raisins and a touch of jaggery."
            )
        ),
        FoodDatabaseItem(
            id = "bn_luchi_alur_dom",
            nameEn = "Luchi & Kosha Alur Dom",
            nameBn = "লুচি ও আলুর দম",
            nameHi = "लूची और आलू दम",
            category = "Bengali Foods",
            servingSizeG = 200.0,
            servingLabel = "2 Luchis + 1 bowl Alur Dom",
            calories = 395,
            protein = 6.4,
            carbohydrates = 49.0,
            fat = 18.8,
            fiber = 3.8,
            sugar = 3.5,
            cookingMethod = "Deep-fried puffed flatbread with spiced baby potatoes",
            cuisine = "Bengali",
            prepTimeMinutes = 35
        ),
        FoodDatabaseItem(
            id = "bn_mishti_doi",
            nameEn = "Mishti Doi (Sweetened Yogurt)",
            nameBn = "মিষ্টি দই",
            nameHi = "मिष्टी दोई",
            category = "Bengali Foods",
            servingSizeG = 100.0,
            servingLabel = "1 small clay cup (100g)",
            calories = 175,
            protein = 4.8,
            carbohydrates = 24.5,
            fat = 6.2,
            fiber = 0.0,
            sugar = 21.0,
            cookingMethod = "Fermented milk sweetened with date palm jaggery (nolen gur)",
            cuisine = "Bengali",
            prepTimeMinutes = 15
        ),
        FoodDatabaseItem(
            id = "bn_rosogolla",
            nameEn = "Baked / Spongy Rosogolla",
            nameBn = "রসগোল্লা",
            nameHi = "रसगुल्ला",
            category = "Bengali Foods",
            servingSizeG = 60.0,
            servingLabel = "1 piece (squeezed syrup)",
            calories = 120,
            protein = 3.2,
            carbohydrates = 22.0,
            fat = 2.1,
            fiber = 0.0,
            sugar = 20.5,
            cookingMethod = "Chhena dumplings poached in light sugar syrup",
            cuisine = "Bengali",
            prepTimeMinutes = 30
        ),
        FoodDatabaseItem(
            id = "bn_chingri_malai",
            nameEn = "Chingri Malai Curry (Prawn Coconut)",
            nameBn = "চিংড়ি মালাইকারি",
            nameHi = "चिंगड़ी मलाई करी",
            category = "Bengali Foods",
            servingSizeG = 160.0,
            servingLabel = "1 bowl (4 prawns)",
            calories = 285,
            protein = 20.5,
            carbohydrates = 8.4,
            fat = 18.6,
            fiber = 1.1,
            sugar = 3.2,
            cookingMethod = "Prawns simmered in rich coconut milk and aromatic spices",
            cuisine = "Bengali",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "120g deveined tiger prawns",
                "1/2 cup thick coconut milk, 1 small onion paste",
                "1 tsp ginger paste, cardamom, cinnamon & bay leaf",
                "1 tbsp mustard oil & 1/2 tsp ghee"
            ),
            recipeSteps = listOf(
                "Lightly sear turmeric-salted prawns for 90 seconds and remove.",
                "Sauté whole spices, onion paste, and ginger until aromatic.",
                "Add coconut milk, simmer gently for 5 minutes, return prawns for 3 minutes, and finish with ghee."
            )
        ),

        // ==================== INDIAN FOODS & HOME RECIPES ====================
        FoodDatabaseItem(
            id = "in_masoor_dal",
            nameEn = "Masoor Dal Tadka (Red Lentil Soup)",
            nameBn = "মসুর ডাল",
            nameHi = "मसूर दाल तड़का",
            category = "Indian Foods",
            servingSizeG = 150.0,
            servingLabel = "1 bowl (150g)",
            calories = 165,
            protein = 10.2,
            carbohydrates = 23.8,
            fat = 3.4,
            fiber = 4.6,
            sugar = 1.4,
            cookingMethod = "Boiled lentils tempered with cumin, garlic, and ghee",
            cuisine = "Indian",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "50g split red lentils (masoor dal), rinsed",
                "1 small tomato & 1/2 onion, finely chopped",
                "2 garlic cloves, 1/2 tsp cumin seeds, 1 dry red chili",
                "1 tsp ghee, 1/4 tsp turmeric & fresh coriander"
            ),
            recipeSteps = listOf(
                "Boil masoor dal with turmeric, salt, and 1.5 cups water until creamy (about 12 minutes).",
                "Heat 1 tsp ghee in a small tadka pan; crackle cumin seeds, chopped garlic, and dry red chili.",
                "Sauté onions and tomatoes until soft, pour the sizzling tadka over dal, garnish with coriander."
            )
        ),
        FoodDatabaseItem(
            id = "in_chicken_biryani",
            nameEn = "Kolkata Chicken Biryani (with Egg & Potato)",
            nameBn = "কলকাতা চিকেন বিরিয়ানি",
            nameHi = "कोलकाता चिकन बिरयानी",
            category = "Indian Foods",
            servingSizeG = 350.0,
            servingLabel = "1 plate (350g)",
            calories = 640,
            protein = 31.5,
            carbohydrates = 74.0,
            fat = 23.2,
            fiber = 3.8,
            sugar = 2.5,
            cookingMethod = "Dum-cooked basmati rice with marinated chicken, potato, and boiled egg",
            cuisine = "Indian",
            prepTimeMinutes = 45,
            ingredients = listOf(
                "150g chicken thighs marinated in curd, ginger-garlic & biryani masala",
                "90g aged basmati rice (parboiled 70%), 1 boiled egg, 1 halved potato",
                "2 tbsp caramelized fried onions (birista), saffron milk",
                "1 tbsp ghee, kewra water & whole spices"
            ),
            recipeSteps = listOf(
                "Cook marinated chicken with browned potato halves until 80% tender.",
                "Layer partially boiled aromatic basmati rice over the chicken gravy with birista, saffron milk, and ghee.",
                "Seal the pot and dum-cook on very low heat for 15 minutes; serve alongside a boiled egg."
            )
        ),
        FoodDatabaseItem(
            id = "in_paneer_butter_masala",
            nameEn = "Paneer Butter Masala (Home Style)",
            nameBn = "পনির বাটার মশলা",
            nameHi = "पनीर बटर मसाला",
            category = "Indian Foods",
            servingSizeG = 180.0,
            servingLabel = "1 bowl (180g)",
            calories = 335,
            protein = 15.2,
            carbohydrates = 14.5,
            fat = 24.0,
            fiber = 2.4,
            sugar = 4.8,
            cookingMethod = "Cottage cheese cubes simmered in spiced tomato-cashew gravy",
            cuisine = "Indian",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "100g fresh paneer cubes",
                "2 ripe tomatoes, 6 soaked cashews, 1/2 onion",
                "1 tsp ginger-garlic paste, 1/2 tsp Kashmiri chili, garam masala",
                "1 tbsp butter, 1 tsp kasuri methi (dried fenugreek)"
            ),
            recipeSteps = listOf(
                "Simmer chopped tomatoes, onion, ginger, garlic, and cashews for 8 minutes, then blend into a smooth puree.",
                "Melt butter in a pan, add Kashmiri chili and puree, and cook until glossy.",
                "Fold in paneer cubes, crushed kasuri methi, and garam masala; simmer 4 minutes."
            )
        ),
        FoodDatabaseItem(
            id = "in_palak_paneer",
            nameEn = "Palak Paneer (Spinach Cottage Cheese)",
            nameBn = "পালং পনির",
            nameHi = "पालक पनीर",
            category = "Indian Foods",
            servingSizeG = 190.0,
            servingLabel = "1 bowl (190g)",
            calories = 265,
            protein = 16.5,
            carbohydrates = 11.2,
            fat = 17.4,
            fiber = 4.2,
            sugar = 2.1,
            cookingMethod = "Blanched spinach puree simmered with garlic, spices, and paneer",
            cuisine = "Indian",
            prepTimeMinutes = 22,
            ingredients = listOf(
                "150g fresh spinach leaves (palak), blanched & pureed",
                "90g low-fat paneer cubes",
                "1 small onion, 3 garlic cloves, 1 green chili",
                "1 tsp cumin, 1/2 tsp garam masala, 2 tsp oil or ghee"
            ),
            recipeSteps = listOf(
                "Blanch spinach in boiling water for 2 minutes, shock in cold water, and blend smooth with green chili.",
                "Sauté cumin, minced garlic, and finely chopped onions in oil until golden.",
                "Add spinach puree, salt, and garam masala; simmer 4 minutes and fold in warm paneer cubes."
            )
        ),
        FoodDatabaseItem(
            id = "in_rajma_chawal",
            nameEn = "Punjabi Rajma Masala with Steamed Rice",
            nameBn = "রাজমা ও ভাত",
            nameHi = "पंजाबी राजमा चावल",
            category = "Indian Foods",
            servingSizeG = 280.0,
            servingLabel = "1 bowl Rajma + Rice (280g)",
            calories = 380,
            protein = 14.2,
            carbohydrates = 64.5,
            fat = 6.8,
            fiber = 9.4,
            sugar = 3.2,
            cookingMethod = "Slow-simmered red kidney beans in onion-tomato masala with rice",
            cuisine = "Indian",
            prepTimeMinutes = 35,
            ingredients = listOf(
                "60g red kidney beans (rajma), soaked overnight & pressure cooked",
                "1 onion, 2 pureed tomatoes, 1 tbsp ginger-garlic paste",
                "1 tsp coriander powder, 1/2 tsp cumin, 1/2 tsp amchur/garam masala",
                "120g cooked basmati rice & 2 tsp oil"
            ),
            recipeSteps = listOf(
                "Pressure-cook soaked rajma with a pinch of salt and bay leaf until soft and creamy.",
                "Sauté onions, ginger-garlic, and tomato puree with spices until oil separates.",
                "Mash a few beans for body, simmer rajma in the masala for 12 minutes, and serve over hot steamed rice."
            )
        ),
        FoodDatabaseItem(
            id = "in_chole_masala",
            nameEn = "Amritsari Chole (Spiced Chickpea Curry)",
            nameBn = "ছোলে মশলা / কাবুলি চানা",
            nameHi = "अमृतसरी छोले मसाला",
            category = "Indian Foods",
            servingSizeG = 190.0,
            servingLabel = "1 bowl (190g)",
            calories = 275,
            protein = 12.8,
            carbohydrates = 39.0,
            fat = 7.4,
            fiber = 8.8,
            sugar = 4.1,
            cookingMethod = "Chickpeas simmered in roasted spice, onion, and tomato gravy",
            cuisine = "Indian",
            prepTimeMinutes = 30,
            ingredients = listOf(
                "75g boiled Kabuli chana (chickpeas)",
                "1 medium onion, 2 tomatoes, 1 tsp ginger juliennes",
                "1 tbsp chole masala, 1/2 tsp roasted cumin, 1/2 tsp amchur (dry mango)",
                "2 tsp oil, tea bag (for color) & fresh cilantro"
            ),
            recipeSteps = listOf(
                "Boil soaked chickpeas with salt and a tea bag until tender, then discard tea bag.",
                "Cook chopped onions, ginger, and tomato puree with chole masala and dry mango powder.",
                "Simmer chickpeas in the masala for 10 minutes, garnish with ginger juliennes and lemon juice."
            )
        ),
        FoodDatabaseItem(
            id = "in_dal_makhani",
            nameEn = "Home-Style Dal Makhani (Black Lentils)",
            nameBn = "ডাল মাখানি",
            nameHi = "दाल मखनी",
            category = "Indian Foods",
            servingSizeG = 180.0,
            servingLabel = "1 bowl (180g)",
            calories = 290,
            protein = 11.5,
            carbohydrates = 30.2,
            fat = 13.5,
            fiber = 6.2,
            sugar = 2.6,
            cookingMethod = "Whole black urad dal slow-simmered with tomato puree and light butter",
            cuisine = "Indian",
            prepTimeMinutes = 40,
            ingredients = listOf(
                "60g whole black urad dal + 15g rajma, soaked overnight",
                "2 pureed tomatoes, 1 tsp ginger-garlic paste",
                "1 tsp Kashmiri red chili, 1/2 tsp kasuri methi",
                "1 tbsp butter & 1 tbsp low-fat cream or milk"
            ),
            recipeSteps = listOf(
                "Pressure-cook soaked urad dal and rajma until soft and mash lightly.",
                "Sauté ginger-garlic and tomato puree with Kashmiri chili in butter until rich.",
                "Simmer lentils slowly for 15–20 minutes, swirl in a spoon of milk/cream and kasuri methi."
            )
        ),
        FoodDatabaseItem(
            id = "in_poha",
            nameEn = "Kanda Batata Poha (Flattened Rice)",
            nameBn = "চিঁড়ের পোলাও",
            nameHi = "कांदा बटाटा पोहा",
            category = "Indian Foods",
            servingSizeG = 160.0,
            servingLabel = "1 plate (160g)",
            calories = 245,
            protein = 5.2,
            carbohydrates = 43.0,
            fat = 5.8,
            fiber = 2.8,
            sugar = 2.2,
            cookingMethod = "Sautéed flattened rice with peanuts, curry leaves, and turmeric",
            cuisine = "Indian",
            prepTimeMinutes = 15,
            ingredients = listOf(
                "65g thick poha (flattened rice), rinsed & drained",
                "1 small onion, 1 small diced potato, 1 tbsp roasted peanuts",
                "1/2 tsp mustard seeds, 8 curry leaves, 1 green chili, 1/4 tsp turmeric",
                "1.5 tsp oil, lemon juice & fresh coriander"
            ),
            recipeSteps = listOf(
                "Rinse poha in a colander for 15 seconds and toss gently with turmeric and salt.",
                "Temper mustard seeds, curry leaves, green chili, and peanuts in oil; sauté onions and diced potatoes until soft.",
                "Fold in soaked poha, steam covered for 2 minutes on low flame, and finish with fresh lemon juice."
            )
        ),
        FoodDatabaseItem(
            id = "in_moong_dal_chilla",
            nameEn = "Stuffed Moong Dal Chilla (Protein Crepe)",
            nameBn = "মুগ ডালের চিলা",
            nameHi = "मूंग दाल चीला",
            category = "Indian Foods",
            servingSizeG = 140.0,
            servingLabel = "2 chillas (140g)",
            calories = 195,
            protein = 12.4,
            carbohydrates = 24.6,
            fat = 4.8,
            fiber = 5.2,
            sugar = 1.6,
            cookingMethod = "Savory yellow moong lentil crepes cooked on non-stick tawa",
            cuisine = "Indian",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "60g soaked yellow moong dal, ground to smooth batter",
                "1/2 inch ginger, 1 green chili, 1/4 tsp cumin seeds",
                "30g grated paneer or finely chopped onion-tomato filling",
                "1 tsp oil for brushing the tawa"
            ),
            recipeSteps = listOf(
                "Blend soaked moong dal with ginger, green chili, cumin, and water into a dosa-like batter.",
                "Spread a ladle of batter onto a warm tawa and drizzle a few drops of oil.",
                "Top with grated paneer/veggies, fold once crisp golden on both sides, and serve with mint chutney."
            )
        ),
        FoodDatabaseItem(
            id = "in_aloo_paratha",
            nameEn = "Aloo Paratha with Plain Curd",
            nameBn = "আলু পরোটা ও টক দই",
            nameHi = "आलू पराठा और दही",
            category = "Indian Foods",
            servingSizeG = 210.0,
            servingLabel = "1 stuffed paratha + 1/2 cup curd",
            calories = 345,
            protein = 8.6,
            carbohydrates = 51.4,
            fat = 11.2,
            fiber = 5.4,
            sugar = 3.2,
            cookingMethod = "Whole wheat flatbread stuffed with spiced mashed potato, tawa-roasted",
            cuisine = "Indian",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "60g whole wheat flour (atta) kneaded into soft dough",
                "1 medium boiled potato, mashed with chili, coriander & amchur",
                "1 tsp ghee for roasting",
                "80g fresh plain curd (dahi) on the side"
            ),
            recipeSteps = listOf(
                "Season mashed boiled potato with green chili, roasted cumin, amchur, and chopped coriander.",
                "Stuff spiced potato ball inside rolled whole-wheat dough and gently roll into a thick disc.",
                "Roast on a hot tawa with 1 tsp ghee until crisp brown spots appear; serve with cool curd."
            )
        ),
        FoodDatabaseItem(
            id = "in_tandoori_chicken_tikka",
            nameEn = "Chicken Tikka Masala (Home Style)",
            nameBn = "চিকেন টিক্কা মশলা",
            nameHi = "चिकन टिक्का मसाला",
            category = "Indian Foods",
            servingSizeG = 200.0,
            servingLabel = "1 bowl (200g)",
            calories = 315,
            protein = 26.8,
            carbohydrates = 12.4,
            fat = 17.5,
            fiber = 2.2,
            sugar = 4.0,
            cookingMethod = "Char-grilled marinated chicken breast simmered in spiced tomato gravy",
            cuisine = "Indian",
            prepTimeMinutes = 30,
            ingredients = listOf(
                "140g boneless chicken breast marinated in yogurt, lemon & tikka spice",
                "2 tomatoes pureed, 1/2 onion finely diced, 1 tsp ginger-garlic paste",
                "1/2 tsp Kashmiri chili, 1/2 tsp garam masala, kasuri methi",
                "2 tsp oil & 1 tsp butter"
            ),
            recipeSteps = listOf(
                "Pan-grill or air-fry yogurt-marinated chicken chunks until charred at the edges.",
                "Sauté onion and ginger-garlic, add tomato puree and spices, and simmer until thick.",
                "Toss charred chicken tikka pieces into the gravy with kasuri methi and cook 4 minutes."
            )
        ),
        FoodDatabaseItem(
            id = "in_khichdi",
            nameEn = "Moong Dal Vegetable Khichdi",
            nameBn = "সবজি মুগ খিচুড়ি",
            nameHi = "मूंग दाल वेजिटेबल खिचड़ी",
            category = "Indian Foods",
            servingSizeG = 230.0,
            servingLabel = "1 warm bowl (230g)",
            calories = 260,
            protein = 10.5,
            carbohydrates = 43.2,
            fat = 5.2,
            fiber = 5.1,
            sugar = 2.2,
            cookingMethod = "One-pot comforting rice, yellow moong dal, and seasonal vegetables",
            cuisine = "Indian",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "40g rice + 40g yellow moong dal, soaked 15 mins",
                "60g mixed carrots, peas & cauliflower florets",
                "1/2 tsp cumin seeds, pinch of hing, turmeric & ginger",
                "1 tsp ghee & 3 cups water"
            ),
            recipeSteps = listOf(
                "Heat ghee in a pressure cooker; temper cumin seeds, hing, and grated ginger.",
                "Stir in veggies, soaked rice, moong dal, turmeric, and salt for 1 minute.",
                "Add 3 cups water and pressure-cook for 3 whistles until soft and porridge-like."
            )
        ),

        // ==================== SOUTH INDIAN FOODS & RECIPES ====================
        FoodDatabaseItem(
            id = "si_masala_dosa",
            nameEn = "Crispy Masala Dosa with Coconut Chutney",
            nameBn = "মসালা ধোসা ও চাটনি",
            nameHi = "मसाला डोसा और नारियल चटनी",
            category = "South Indian Foods",
            servingSizeG = 220.0,
            servingLabel = "1 large dosa + chutney",
            calories = 310,
            protein = 7.8,
            carbohydrates = 49.5,
            fat = 8.9,
            fiber = 4.2,
            sugar = 2.4,
            cookingMethod = "Fermented rice-urad batter crepe filled with spiced potato masala",
            cuisine = "South Indian",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "120g fermented dosa batter (parboiled rice + urad dal)",
                "80g boiled potato masala with mustard seeds, curry leaves & turmeric",
                "30g fresh coconut-roasted chana chutney",
                "1 tsp ghee or sesame oil for crisping"
            ),
            recipeSteps = listOf(
                "Temper mustard seeds, urad dal, curry leaves, green chili, and onions; toss with turmeric and mashed potatoes.",
                "Ladle fermented dosa batter onto a hot cast-iron tawa in a thin spiral and drizzle 1 tsp ghee.",
                "Place potato masala in the center once golden crisp, fold cleanly, and serve with coconut chutney."
            )
        ),
        FoodDatabaseItem(
            id = "si_idli_sambar",
            nameEn = "Steamed Idli with Drumstick Sambar",
            nameBn = "ইডলি ও সাম্বার",
            nameHi = "इडली सांभर",
            category = "South Indian Foods",
            servingSizeG = 220.0,
            servingLabel = "2 Idlis + 1 bowl Sambar",
            calories = 230,
            protein = 8.4,
            carbohydrates = 41.2,
            fat = 3.2,
            fiber = 4.8,
            sugar = 2.6,
            cookingMethod = "Steamed fermented rice-lentil cakes with toor dal vegetable stew",
            cuisine = "South Indian",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "2 steamed idlis (from fermented rice & urad dal batter)",
                "40g cooked toor dal",
                "50g drumstick, carrot & tomato + 1 tsp tamarind pulp",
                "1 tbsp sambar powder, mustard seeds, curry leaves & hing"
            ),
            recipeSteps = listOf(
                "Pour fermented idli batter into greased molds and steam for 10–12 minutes until fluffy.",
                "Simmer drumstick, tomatoes, and tamarind water with sambar powder, then whisk in cooked toor dal.",
                "Temper mustard seeds, dry chili, and curry leaves over the sambar and serve piping hot with idlis."
            )
        ),
        FoodDatabaseItem(
            id = "si_medu_vada",
            nameEn = "Crispy Medu Vada with Sambar",
            nameBn = "মেদু বড়া ও সাম্বার",
            nameHi = "मेदू वड़ा और सांभर",
            category = "South Indian Foods",
            servingSizeG = 160.0,
            servingLabel = "2 vadas + 1/2 cup sambar",
            calories = 285,
            protein = 8.2,
            carbohydrates = 33.4,
            fat = 13.0,
            fiber = 4.5,
            sugar = 1.8,
            cookingMethod = "Fluffy urad dal fritters seasoned with pepper and curry leaves",
            cuisine = "South Indian",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "65g soaked urad dal, whipped to airy batter",
                "1/2 tsp crushed black peppercorns, curry leaves, chopped ginger",
                "1 tbsp chopped onion, pinch of salt",
                "Oil for shallow/deep frying + sambar for dipping"
            ),
            recipeSteps = listOf(
                "Grind soaked urad dal with minimal ice-cold water into a fluffy, thick batter and aerate by whisking.",
                "Mix in crushed pepper, chopped curry leaves, ginger, and salt.",
                "Shape wet-handed rings, fry on medium heat until crisp golden, and serve with warm sambar."
            )
        ),
        FoodDatabaseItem(
            id = "si_ven_pongal",
            nameEn = "Ven Pongal (Ghee Pepper Khara Pongal)",
            nameBn = "ভেন পোঙ্গাল",
            nameHi = "वेन पोंगल",
            category = "South Indian Foods",
            servingSizeG = 200.0,
            servingLabel = "1 bowl (200g)",
            calories = 295,
            protein = 8.6,
            carbohydrates = 42.8,
            fat = 9.8,
            fiber = 3.1,
            sugar = 0.8,
            cookingMethod = "Creamy rice and roasted moong dal tempered with ghee, pepper, and cashews",
            cuisine = "South Indian",
            prepTimeMinutes = 22,
            ingredients = listOf(
                "50g raw rice + 25g dry-roasted yellow moong dal",
                "1 tsp whole black peppercorns, 1 tsp cumin seeds",
                "1 inch grated ginger, 6 cashews, 8 curry leaves",
                "1 tbsp ghee & pinch of hing"
            ),
            recipeSteps = listOf(
                "Dry-roast moong dal briefly, then pressure-cook with rice, salt, and 3.5 cups water until mashable.",
                "Heat ghee in a pan; fry cashews golden, then crackle cumin, whole peppercorns, ginger, hing, and curry leaves.",
                "Fold the aromatic tempering into the hot rice-dal mash and serve with coconut chutney."
            )
        ),
        FoodDatabaseItem(
            id = "si_rava_upma",
            nameEn = "Vegetable Rava Upma (Semolina Porridge)",
            nameBn = "সবজি সুজির উপমা",
            nameHi = "वेज रवा उपमा",
            category = "South Indian Foods",
            servingSizeG = 180.0,
            servingLabel = "1 bowl (180g)",
            calories = 220,
            protein = 5.8,
            carbohydrates = 34.5,
            fat = 6.4,
            fiber = 3.2,
            sugar = 2.0,
            cookingMethod = "Roasted semolina cooked with tempered mustard, urad dal, and veggies",
            cuisine = "South Indian",
            prepTimeMinutes = 15,
            ingredients = listOf(
                "55g roasted rava (coarse semolina)",
                "50g finely diced carrot, green peas & onion",
                "1/2 tsp mustard seeds, 1 tsp urad dal, curry leaves, green chili",
                "1.5 tsp oil/ghee & 1.5 cups boiling water"
            ),
            recipeSteps = listOf(
                "Dry-roast rava for 3 minutes until fragrant and set aside.",
                "Temper mustard seeds, urad dal, curry leaves, and chili in oil; sauté onions, carrots, and peas.",
                "Pour boiling water, slowly rain in roasted rava while stirring lump-free, and steam covered 3 minutes."
            )
        ),
        FoodDatabaseItem(
            id = "si_onion_uttapam",
            nameEn = "Vegetable & Onion Uttapam",
            nameBn = "অনিয়ন উত্তাপম",
            nameHi = "प्याज़ और सब्ज़ी उत्तपम",
            category = "South Indian Foods",
            servingSizeG = 175.0,
            servingLabel = "1 thick uttapam (175g)",
            calories = 255,
            protein = 7.4,
            carbohydrates = 41.5,
            fat = 6.2,
            fiber = 3.6,
            sugar = 2.8,
            cookingMethod = "Thick fermented dosa pancake topped with onion, tomato, and coriander",
            cuisine = "South Indian",
            prepTimeMinutes = 15,
            ingredients = listOf(
                "120g fermented dosa/idli batter",
                "30g finely chopped onion, 25g diced tomato, grated carrot",
                "1 green chili, fresh cilantro",
                "1 tsp oil for roasting"
            ),
            recipeSteps = listOf(
                "Pour a thick round ladle of fermented batter onto a medium-hot tawa without spreading too thin.",
                "Scatter chopped onions, tomatoes, carrots, chili, and coriander on top and press gently.",
                "Drizzle oil around edges, cover 2 minutes, flip once golden, and roast another minute."
            )
        ),
        FoodDatabaseItem(
            id = "si_curd_rice",
            nameEn = "Tempered Curd Rice (Thayir Sadam)",
            nameBn = "টক দই ভাত (কার্ড রাইস)",
            nameHi = "दही चावल (थयिर सादम)",
            category = "South Indian Foods",
            servingSizeG = 200.0,
            servingLabel = "1 cooling bowl (200g)",
            calories = 240,
            protein = 6.9,
            carbohydrates = 36.8,
            fat = 7.1,
            fiber = 1.4,
            sugar = 4.2,
            cookingMethod = "Soft-mashed rice folded with probiotic curd and South Indian tempering",
            cuisine = "South Indian",
            prepTimeMinutes = 12,
            ingredients = listOf(
                "120g soft-cooked mashed rice (cooled)",
                "80g whisked fresh plain curd + 2 tbsp milk",
                "1/2 tsp mustard seeds, urad dal, curry leaves, grated ginger",
                "1 tsp oil & 1 tbsp pomegranate seeds"
            ),
            recipeSteps = listOf(
                "Mash warm cooked rice slightly, let cool, and mix smoothly with fresh curd, milk, and salt.",
                "Temper mustard seeds, urad dal, chopped ginger, green chili, and curry leaves in 1 tsp oil.",
                "Pour tempering over curd rice, garnish with pomegranate and cilantro, and serve chilled or room temp."
            )
        ),
        FoodDatabaseItem(
            id = "si_lemon_rice",
            nameEn = "South Indian Lemon Rice (Chitranna)",
            nameBn = "লেমন রাইস",
            nameHi = "नींबू चावल (चित्रान्ना)",
            category = "South Indian Foods",
            servingSizeG = 190.0,
            servingLabel = "1 bowl (190g)",
            calories = 265,
            protein = 5.2,
            carbohydrates = 44.0,
            fat = 7.4,
            fiber = 2.0,
            sugar = 0.9,
            cookingMethod = "Fluffy rice tossed with roasted peanuts, turmeric, curry leaves, and lemon",
            cuisine = "South Indian",
            prepTimeMinutes = 15,
            ingredients = listOf(
                "150g cooked cooled rice",
                "1 tbsp roasted peanuts, 1 tsp chana dal & urad dal",
                "1/2 tsp mustard seeds, 1/4 tsp turmeric, curry leaves, dry chili",
                "1.5 tbsp fresh lemon juice & 1.5 tsp sesame oil"
            ),
            recipeSteps = listOf(
                "Heat sesame oil; fry peanuts, chana dal, urad dal, mustard seeds, dry chili, and curry leaves until crisp.",
                "Turn off heat, stir in turmeric and pinch of hing, then toss with fluffy cooked rice and salt.",
                "Squeeze fresh lemon juice over the rice, mix lightly, and rest 5 minutes for flavors to meld."
            )
        ),
        FoodDatabaseItem(
            id = "si_appam_veg_stew",
            nameEn = "Kerala Appam with Coconut Veg Stew",
            nameBn = "কেরালা আপ্পম ও সবজি স্টু",
            nameHi = "केरला अप्पम और वेज स्टू",
            category = "South Indian Foods",
            servingSizeG = 230.0,
            servingLabel = "2 Appams + 1 bowl Stew",
            calories = 305,
            protein = 6.4,
            carbohydrates = 46.5,
            fat = 10.6,
            fiber = 4.2,
            sugar = 3.8,
            cookingMethod = "Lacy fermented rice hoppers paired with mild spiced coconut vegetable stew",
            cuisine = "South Indian",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "120g fermented appam rice-coconut batter",
                "80g carrot, green beans, peas & potato",
                "1/2 cup thin & thick coconut milk",
                "Cardamom, cloves, cinnamon, curry leaves & 1 tsp coconut oil"
            ),
            recipeSteps = listOf(
                "Simmer vegetables with whole spices, ginger, and thin coconut milk until tender, then finish with thick coconut milk and curry leaves.",
                "Swirl appam batter in a heated appachatti pan so the center stays soft and edges turn lacy crisp.",
                "Cover for 2 minutes until cooked through and serve warm with fragrant coconut stew."
            )
        ),
        FoodDatabaseItem(
            id = "si_pesarattu",
            nameEn = "Pesarattu (Green Moong Protein Dosa)",
            nameBn = "পেসারাট্টু (সবুজ মুগ ধোসা)",
            nameHi = "पेसरट्टू (हरा मूंग डोसा)",
            category = "South Indian Foods",
            servingSizeG = 150.0,
            servingLabel = "2 crisp pesarattu crepes",
            calories = 210,
            protein = 11.2,
            carbohydrates = 30.8,
            fat = 4.4,
            fiber = 6.5,
            sugar = 1.5,
            cookingMethod = "Soaked whole green gram batter spiced with ginger and green chili",
            cuisine = "South Indian",
            prepTimeMinutes = 18,
            ingredients = listOf(
                "70g whole green moong dal, soaked 4 hours",
                "1 inch ginger, 2 green chilies, 1/2 tsp cumin seeds",
                "2 tbsp finely chopped onion & fresh coriander",
                "1 tsp oil for griddling"
            ),
            recipeSteps = listOf(
                "Grind soaked green moong dal with ginger, green chilies, cumin, and salt into a smooth batter (no fermentation needed).",
                "Spread thinly on a hot tawa, sprinkle chopped onions, and drizzle a few drops of oil.",
                "Cook until crisp golden underneath, fold, and serve with ginger-tomato chutney."
            )
        ),
        FoodDatabaseItem(
            id = "si_rasam_rice",
            nameEn = "Tomato Pepper Rasam with Rice",
            nameBn = "টমেটো রসম ও ভাত",
            nameHi = "टमाटर काली मिर्च रसम और चावल",
            category = "South Indian Foods",
            servingSizeG = 240.0,
            servingLabel = "1 bowl Rasam + Rice",
            calories = 215,
            protein = 5.1,
            carbohydrates = 41.0,
            fat = 3.2,
            fiber = 2.6,
            sugar = 3.0,
            cookingMethod = "Tangy tamarind-tomato broth infused with crushed black pepper and garlic",
            cuisine = "South Indian",
            prepTimeMinutes = 18,
            ingredients = listOf(
                "2 ripe tomatoes crushed, 1 tsp tamarind paste",
                "1 tsp crushed black pepper-cumin-garlic paste",
                "1 tsp ghee, mustard seeds, curry leaves & fresh coriander",
                "120g hot steamed rice"
            ),
            recipeSteps = listOf(
                "Boil crushed tomatoes, tamarind water, turmeric, and salt for 6 minutes until raw tang mellows.",
                "Add crushed black pepper, cumin, and garlic; bring just to a gentle froth and turn off heat.",
                "Temper mustard seeds and curry leaves in ghee, top with coriander, and pour over steamed rice."
            )
        ),
        FoodDatabaseItem(
            id = "si_malabar_fish_curry",
            nameEn = "Malabar Coconut Fish Curry",
            nameBn = "মালাবার ফিশ কারি",
            nameHi = "मालाबार नारियल मछली करी",
            category = "South Indian Foods",
            servingSizeG = 180.0,
            servingLabel = "1 bowl (1 fish steak + curry)",
            calories = 275,
            protein = 23.0,
            carbohydrates = 6.5,
            fat = 17.4,
            fiber = 1.6,
            sugar = 2.0,
            cookingMethod = "Fish simmered in coconut milk, Malabar tamarind, and curry leaves",
            cuisine = "South Indian",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "130g firm white fish or mackerel",
                "1/2 cup coconut milk, 1 chopped tomato, 4 shallots",
                "1 tsp kudampuli (or tamarind), 1 tsp chili-coriander-turmeric",
                "1 tbsp coconut oil, fenugreek seeds & curry leaves"
            ),
            recipeSteps = listOf(
                "Sauté fenugreek seeds, sliced shallots, ginger, and curry leaves in coconut oil.",
                "Stir in spice powders, tomato, and tamarind water; simmer 5 minutes before sliding in fish pieces.",
                "Cook gently 7 minutes, finish with coconut milk without boiling hard, and rest before serving."
            )
        ),

        // ==================== NEPALI FOODS & RECIPES ====================
        FoodDatabaseItem(
            id = "np_dal_bhat_tarkari",
            nameEn = "Nepali Dal Bhat Tarkari Thali",
            nameBn = "নেপালি ডাল ভাত তরকারি",
            nameHi = "नेपाली दाल भात तरकारी",
            category = "Nepali Foods",
            servingSizeG = 380.0,
            servingLabel = "1 traditional plate (rice, dal, tarkari, achar)",
            calories = 495,
            protein = 16.8,
            carbohydrates = 84.0,
            fat = 9.6,
            fiber = 9.2,
            sugar = 3.8,
            cookingMethod = "Steamed rice with jimbu-tempered black/red dal, seasonal tarkari & tomato achar",
            cuisine = "Nepali",
            prepTimeMinutes = 35,
            ingredients = listOf(
                "160g steamed rice (bhat) + 120g jimbu-tempered kaalo/musuro dal",
                "80g cauliflower-potato-green bean tarkari (spiced dry curry)",
                "20g roasted tomato-sesame-timur (Sichuan pepper) achar",
                "1 tsp ghee & crushed garlic"
            ),
            recipeSteps = listOf(
                "Boil lentils with turmeric and salt, then temper in 1 tsp ghee with dried Himalayan jimbu herb and garlic.",
                "Sauté cauliflower, potato, and beans with cumin, turmeric, and ginger-garlic for the tarkari.",
                "Char tomatoes and grind with toasted sesame seeds, chili, and timur for fresh achar; plate everything warm."
            )
        ),
        FoodDatabaseItem(
            id = "np_chicken_momo",
            nameEn = "Nepali Steamed Momo with Tomato-Sesame Achar",
            nameBn = "নেপালি স্টিমড মোমো ও চাটনি",
            nameHi = "नेपाली स्टीम्ड मोमो और अचार",
            category = "Nepali Foods",
            servingSizeG = 210.0,
            servingLabel = "8 dumplings + spicy tomato-sesame achar",
            calories = 285,
            protein = 17.4,
            carbohydrates = 36.2,
            fat = 7.6,
            fiber = 2.4,
            sugar = 2.2,
            cookingMethod = "Hand-pleated steamed dumplings with roasted tomato-sesame-timur dip",
            cuisine = "Nepali",
            prepTimeMinutes = 35,
            ingredients = listOf(
                "90g minced lean chicken (or chopped cabbage-paneer for veg)",
                "50g finely chopped onion, scallions, ginger, garlic & cilantro",
                "1/2 tsp momo masala (cumin, coriander, turmeric) & 1 tsp oil",
                "70g wheat flour dough wrappers + roasted tomato-sesame achar"
            ),
            recipeSteps = listOf(
                "Mix minced meat/veggies with chopped onion, ginger-garlic, cilantro, momo masala, and a splash of broth so filling stays juicy.",
                "Roll thin circular wrappers, spoon 1 tbsp filling inside, and pleat tightly into crescent or round momos.",
                "Steam over boiling water for 12–14 minutes and serve with roasted tomato, toasted sesame, and timur dipping achar."
            )
        ),
        FoodDatabaseItem(
            id = "np_veg_thukpa",
            nameEn = "Himalayan Chicken & Veg Thukpa Noodle Soup",
            nameBn = "নেপালি চিকেন ও ভেজ থুকপা",
            nameHi = "हिमालयन थुकपा नूडल सूप",
            category = "Nepali Foods",
            servingSizeG = 300.0,
            servingLabel = "1 steaming bowl (300g)",
            calories = 310,
            protein = 18.2,
            carbohydrates = 41.5,
            fat = 7.8,
            fiber = 4.4,
            sugar = 3.2,
            cookingMethod = "Spiced Himalayan noodle broth simmered with shredded chicken and crisp greens",
            cuisine = "Nepali",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "70g wheat noodles, 70g shredded chicken breast (or tofu)",
                "80g shredded cabbage, carrot juliennes, greens & spring onion",
                "1 pureed tomato, 1 tsp ginger-garlic, 1/2 tsp timur & cumin",
                "1 tsp oil, soy/lemon juice & 2 cups light stock"
            ),
            recipeSteps = listOf(
                "Sauté ginger-garlic, onion, and tomato with cumin, turmeric, and crushed timur in 1 tsp oil.",
                "Pour 2 cups chicken/veg stock, add carrots, cabbage, and shredded chicken, and simmer 8 minutes.",
                "Slide in cooked wheat noodles and fresh greens, simmer 2 minutes, and finish with lemon juice and chili."
            )
        ),
        FoodDatabaseItem(
            id = "np_sel_roti",
            nameEn = "Sel Roti (Traditional Nepali Rice Ring Bread)",
            nameBn = "নেপালি সেল রুটি",
            nameHi = "नेपाली सेल रोटी",
            category = "Nepali Foods",
            servingSizeG = 95.0,
            servingLabel = "2 medium rings (95g)",
            calories = 245,
            protein = 3.8,
            carbohydrates = 41.2,
            fat = 7.4,
            fiber = 1.6,
            sugar = 9.5,
            cookingMethod = "Coarsely ground soaked rice batter fried into crisp golden rings",
            cuisine = "Nepali",
            prepTimeMinutes = 30,
            ingredients = listOf(
                "65g soaked short-grain rice, ground coarse",
                "1 tbsp ripe mashed banana or milk, 1 tsp ghee",
                "1.5 tsp sugar, 1/4 tsp ground cardamom & clove",
                "Oil/ghee for shallow-frying crisp rings"
            ),
            recipeSteps = listOf(
                "Grind soaked rice with ghee, cardamom, sugar, and a splash of water/milk into a pouring-thick coarse batter; rest 1 hour.",
                "Pour batter in steady circular rings into medium-hot oil.",
                "Fry 2 minutes per side until puffed and deep golden brown; drain and serve warm with aloo achar."
            )
        ),
        FoodDatabaseItem(
            id = "np_gundruk_jhol",
            nameEn = "Gundruk ko Jhol (Fermented Greens & Soybean Soup)",
            nameBn = "গুন্দ্রুক কো ঝোল (নেপালি স্যুপ)",
            nameHi = "गुंद्रुक को झोल (नेपाली सूप)",
            category = "Nepali Foods",
            servingSizeG = 200.0,
            servingLabel = "1 bowl (200g)",
            calories = 135,
            protein = 7.6,
            carbohydrates = 14.5,
            fat = 5.2,
            fiber = 5.8,
            sugar = 2.0,
            cookingMethod = "Tangy sun-dried fermented mustard greens simmered with roasted soybeans",
            cuisine = "Nepali",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "25g gundruk (dried fermented leafy greens), soaked 10 mins",
                "20g dry-roasted soybeans (bhatmas), 1 diced potato",
                "1 chopped tomato, 3 garlic cloves, 1/2 tsp turmeric & cumin",
                "1 tsp mustard oil & green chili"
            ),
            recipeSteps = listOf(
                "Roast soybeans until crunchy and crackly; soak dried gundruk briefly and squeeze excess water.",
                "Sauté garlic, onion, and gundruk in 1 tsp mustard oil with turmeric and cumin for 3 minutes.",
                "Add diced potato, tomato, and 2 cups water; simmer 10 minutes and top with crunchy roasted soybeans."
            )
        ),
        FoodDatabaseItem(
            id = "np_aloo_tama",
            nameEn = "Aloo Tama (Potato, Bamboo Shoot & Black-Eyed Pea Curry)",
            nameBn = "আলু তামা (নেপালি বাঁশ কোঁড়ল কারি)",
            nameHi = "आलू तामा (नेपाली बांस और लोबिया करी)",
            category = "Nepali Foods",
            servingSizeG = 200.0,
            servingLabel = "1 bowl (200g)",
            calories = 195,
            protein = 6.8,
            carbohydrates = 31.4,
            fat = 4.8,
            fiber = 6.1,
            sugar = 2.4,
            cookingMethod = "Tangy fermented bamboo shoots simmered with potatoes and black-eyed peas",
            cuisine = "Nepali",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "80g cubed potatoes, 45g boiled black-eyed peas (bodi)",
                "45g fermented bamboo shoots (tama)",
                "1 chopped tomato, 1 tsp ginger-garlic, fenugreek seeds",
                "1 tsp mustard oil, turmeric & cumin"
            ),
            recipeSteps = listOf(
                "Temper fenugreek seeds in mustard oil until dark golden, then sauté onions, ginger-garlic, and potatoes.",
                "Add fermented bamboo shoots (tama), turmeric, cumin, and tomatoes; cook 4 minutes.",
                "Fold in boiled black-eyed peas and 1.5 cups water; simmer 10 minutes until tangy and flavorful."
            )
        ),
        FoodDatabaseItem(
            id = "np_sekuwa",
            nameEn = "Nepali Chicken Sekuwa (Charcoal Spiced Skewers)",
            nameBn = "নেপালি চিকেন সেকুয়া",
            nameHi = "नेपाली चिकन सेकुवा",
            category = "Nepali Foods",
            servingSizeG = 160.0,
            servingLabel = "160g grilled skewers + puffed rice",
            calories = 255,
            protein = 29.4,
            carbohydrates = 5.6,
            fat = 12.2,
            fiber = 1.1,
            sugar = 1.4,
            cookingMethod = "Mustard-oil and timur marinated meat skewers roasted over open flame/grill",
            cuisine = "Nepali",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "150g boneless chicken thigh/breast cubes",
                "1 tbsp mustard oil, 1 tbsp ginger-garlic paste, lemon juice",
                "1/2 tsp crushed timur (Sichuan pepper), cumin & chili",
                "2 tbsp plain yogurt & salt"
            ),
            recipeSteps = listOf(
                "Whisk yogurt, mustard oil, ginger-garlic, timur, cumin, turmeric, and lemon juice; marinate chicken 30 mins.",
                "Thread onto skewers and grill or pan-sear over high heat, basting lightly with leftover marinade.",
                "Cook 10–12 minutes turning until smoky charred edges form; serve with chiura (beaten rice) and tomato achar."
            )
        ),
        FoodDatabaseItem(
            id = "np_chatamari",
            nameEn = "Newari Chatamari (Nepali Rice Crepe)",
            nameBn = "নেওয়ারি চাতামারি",
            nameHi = "नेवारी चातामारी",
            category = "Nepali Foods",
            servingSizeG = 170.0,
            servingLabel = "1 loaded crepe (170g)",
            calories = 270,
            protein = 11.8,
            carbohydrates = 36.0,
            fat = 8.4,
            fiber = 2.6,
            sugar = 1.8,
            cookingMethod = "Thin crisp rice-flour crepe topped with spiced egg/mince and greens",
            cuisine = "Nepali",
            prepTimeMinutes = 18,
            ingredients = listOf(
                "65g fine rice flour whisked with water & pinch of salt",
                "1 beaten egg (or 50g spiced minced chicken/paneer)",
                "30g finely chopped onion, tomato, green peas & cilantro",
                "1 tsp oil & pinch of black pepper"
            ),
            recipeSteps = listOf(
                "Whisk rice flour with water into a thin pouring batter and ladle onto a lightly oiled hot non-stick pan.",
                "Immediately top with chopped onions, tomatoes, peas, and drizzle beaten egg or spiced mince evenly.",
                "Cover with a lid for 4–5 minutes on low-medium flame until bottom turns crisp and toppings set."
            )
        ),
        FoodDatabaseItem(
            id = "np_kwati",
            nameEn = "Nepali Kwati (9-Sprouted Mixed Bean Soup)",
            nameBn = "নেপালি কোয়াটি (অঙ্কুরিত ডাল স্যুপ)",
            nameHi = "नेपाली क्वाटी (अंकुरित दाल सूप)",
            category = "Nepali Foods",
            servingSizeG = 210.0,
            servingLabel = "1 protein-rich bowl (210g)",
            calories = 225,
            protein = 14.5,
            carbohydrates = 33.8,
            fat = 3.6,
            fiber = 9.8,
            sugar = 2.5,
            cookingMethod = "Sprouted black gram, mung, chickpeas, and kidney beans spiced with ajwain",
            cuisine = "Nepali",
            prepTimeMinutes = 30,
            ingredients = listOf(
                "100g mixed sprouted beans (mung, chana, black urad, rajma, soy)",
                "1/2 tsp carom seeds (jwano/ajwain), bay leaf, jimbu",
                "1 tomato, 1 tsp ginger-garlic paste, cumin & turmeric",
                "1 tsp mustard oil & 2.5 cups water"
            ),
            recipeSteps = listOf(
                "Temper jwano (carom seeds), cumin, and bay leaf in mustard oil until fragrant.",
                "Add sprouted mixed beans, ginger-garlic, turmeric, and chopped tomato; sauté 4 minutes.",
                "Add 2.5 cups water and pressure-cook 3 whistles until beans are tender and broth is rich."
            )
        ),
        FoodDatabaseItem(
            id = "np_yomari",
            nameEn = "Yomari (Steamed Rice Dumpling with Chaku)",
            nameBn = "নেপালি ইয়োমারি",
            nameHi = "नेपाली योमारी",
            category = "Nepali Foods",
            servingSizeG = 110.0,
            servingLabel = "2 steamed dumplings (110g)",
            calories = 235,
            protein = 4.2,
            carbohydrates = 45.8,
            fat = 4.1,
            fiber = 1.9,
            sugar = 16.5,
            cookingMethod = "Conical rice-flour dumplings filled with melted jaggery and toasted sesame",
            cuisine = "Nepali",
            prepTimeMinutes = 30,
            ingredients = listOf(
                "65g fine rice flour kneaded with hot water into smooth dough",
                "25g melted molasses/jaggery (chaku)",
                "1 tbsp roasted black/white sesame seeds (crushed)",
                "Pinch of cardamom"
            ),
            recipeSteps = listOf(
                "Knead rice flour with boiling water into a pliable dough and shape hollow fig-like cones with your thumb.",
                "Mix melted jaggery with crushed toasted sesame seeds and cardamom; spoon inside each cone and seal tip.",
                "Steam in a steamer basket for 10–12 minutes until glossy and cooked through."
            )
        ),
        FoodDatabaseItem(
            id = "np_dhido_tarkari",
            nameEn = "Millet / Buckwheat Dhido with Chicken Gravy",
            nameBn = "নেপালি ঢিঁড়ো ও চিকেন ঝোল",
            nameHi = "नेपाली ढिंडो और चिकन झोल",
            category = "Nepali Foods",
            servingSizeG = 280.0,
            servingLabel = "1 portion Dhido + gravy (280g)",
            calories = 340,
            protein = 15.6,
            carbohydrates = 51.2,
            fat = 7.8,
            fiber = 6.4,
            sugar = 1.5,
            cookingMethod = "Traditional whisked millet/buckwheat flour porridge served with spiced broth",
            cuisine = "Nepali",
            prepTimeMinutes = 25,
            ingredients = listOf(
                "75g finger millet (kodo) or buckwheat (phapar) flour",
                "1.5 cups boiling water + 1/2 tsp ghee",
                "120g Nepali spiced local chicken/soybean jhol",
                "1 tbsp gundruk or tomato-timur achar"
            ),
            recipeSteps = listOf(
                "Bring water with 1/2 tsp ghee to a rolling boil in a heavy pan.",
                "Gradually sprinkle millet/buckwheat flour while stirring vigorously with a wooden paddle (dabilo) until smooth and lump-free.",
                "Steam covered on very low heat for 3 minutes and serve warm alongside spiced jhol and achar."
            )
        ),
        FoodDatabaseItem(
            id = "np_choyela",
            nameEn = "Newari Grilled Chicken Choyela",
            nameBn = "নেপালি চিকেন ছোয়েলা",
            nameHi = "नेपाली चिकन छोएला",
            category = "Nepali Foods",
            servingSizeG = 160.0,
            servingLabel = "1 bowl spiced grilled strips (160g)",
            calories = 245,
            protein = 27.5,
            carbohydrates = 4.8,
            fat = 12.6,
            fiber = 1.4,
            sugar = 1.0,
            cookingMethod = "Grilled chicken tossed in smoked mustard oil, fenugreek, ginger, and chili",
            cuisine = "Nepali",
            prepTimeMinutes = 22,
            ingredients = listOf(
                "140g grilled/charred chicken breast strips",
                "1 tbsp minced ginger-garlic, 1 tsp chili flakes, scallions",
                "1 tbsp raw mustard oil tempered with fenugreek seeds & turmeric",
                "1/2 tsp crushed timur & lemon juice"
            ),
            recipeSteps = listOf(
                "Char-grill chicken pieces until cooked through and slice into bite-sized strips.",
                "Toss chicken strips in a bowl with ginger, garlic, chili flakes, chopped scallions, salt, and timur.",
                "Heat mustard oil with fenugreek seeds until smoking, turn off heat, stir in turmeric, and pour sizzling over the chicken."
            )
        ),

        // ==================== RICE AND GRAINS ====================
        FoodDatabaseItem(
            id = "grain_steamed_rice",
            nameEn = "Cooked White Steamed Rice (Bhaat)",
            nameBn = "সাদা ভাত",
            nameHi = "उबले हुए चावल (भात)",
            category = "Rice and Grains",
            servingSizeG = 150.0,
            servingLabel = "1 medium bowl (150g)",
            calories = 195,
            protein = 4.0,
            carbohydrates = 42.8,
            fat = 0.4,
            fiber = 0.6,
            sugar = 0.1,
            cookingMethod = "Boiled/steamed with starch drained",
            cuisine = "Indian"
        ),
        FoodDatabaseItem(
            id = "grain_brown_rice",
            nameEn = "Cooked Brown / Dheki Chata Rice",
            nameBn = "ঢেঁকি ছাঁটা লাল চালের ভাত",
            nameHi = "ब्राउन राइस",
            category = "Rice and Grains",
            servingSizeG = 150.0,
            servingLabel = "1 medium bowl (150g)",
            calories = 170,
            protein = 4.5,
            carbohydrates = 35.2,
            fat = 1.4,
            fiber = 2.8,
            sugar = 0.4,
            cookingMethod = "Boiled whole grain rice",
            cuisine = "Indian"
        ),
        FoodDatabaseItem(
            id = "grain_roti",
            nameEn = "Whole Wheat Roti / Chapati (Phulka)",
            nameBn = "আটার রুটি",
            nameHi = "गेहूं की रोटी / चपाती",
            category = "Rice and Grains",
            servingSizeG = 40.0,
            servingLabel = "1 medium roti (no oil)",
            calories = 104,
            protein = 3.4,
            carbohydrates = 20.2,
            fat = 0.9,
            fiber = 2.9,
            sugar = 0.6,
            cookingMethod = "Dry roasted on tawa",
            cuisine = "Indian"
        ),
        FoodDatabaseItem(
            id = "grain_oats_porridge",
            nameEn = "Masala Oats / Milk Oats Bowl",
            nameBn = "ওটস পরিজ",
            nameHi = "मसाला ओट्स / दलिया",
            category = "Rice and Grains",
            servingSizeG = 200.0,
            servingLabel = "1 bowl (200g cooked)",
            calories = 185,
            protein = 6.8,
            carbohydrates = 31.0,
            fat = 3.6,
            fiber = 4.5,
            sugar = 2.4,
            cookingMethod = "Boiled rolled oats",
            cuisine = "Indian",
            prepTimeMinutes = 10,
            ingredients = listOf(
                "45g rolled oats, 1/4 cup chopped carrots & peas",
                "1/4 tsp cumin, turmeric, black pepper & ginger",
                "1.25 cups water or milk"
            ),
            recipeSteps = listOf(
                "Sauté cumin, ginger, and finely chopped veggies for 2 minutes in a pan.",
                "Add rolled oats, turmeric, salt, and 1.25 cups water; simmer 5 minutes until creamy."
            )
        ),
        FoodDatabaseItem(
            id = "grain_bread",
            nameEn = "Whole Wheat Bread Slices",
            nameBn = "ব্রাউন ব্রেড",
            nameHi = "ब्राउन ब्रेड",
            category = "Rice and Grains",
            servingSizeG = 60.0,
            servingLabel = "2 slices (60g)",
            calories = 152,
            protein = 6.0,
            carbohydrates = 28.4,
            fat = 1.8,
            fiber = 3.6,
            sugar = 2.8,
            cookingMethod = "Baked whole wheat loaf",
            cuisine = "International"
        ),

        // ==================== MEAT AND FISH ====================
        FoodDatabaseItem(
            id = "meat_chicken_curry",
            nameEn = "Home-style Chicken Curry (Murgir Jhol)",
            nameBn = "মুরগির ঝোল",
            nameHi = "घरेलू चिकन करी",
            category = "Meat and Fish",
            servingSizeG = 200.0,
            servingLabel = "1 bowl (3 pieces + potato)",
            calories = 295,
            protein = 28.6,
            carbohydrates = 11.4,
            fat = 14.8,
            fiber = 1.9,
            sugar = 2.1,
            cookingMethod = "Light onion-garlic-ginger gravy with chicken pieces",
            cuisine = "Indian",
            prepTimeMinutes = 30,
            ingredients = listOf(
                "140g bone-in chicken, 1 small halved potato",
                "1 sliced onion, 1 tomato, 1 tbsp ginger-garlic paste",
                "1 tsp cumin-coriander-turmeric, whole garam masala, 1 tbsp oil"
            ),
            recipeSteps = listOf(
                "Sauté whole spices and sliced onions in oil until golden brown, then add ginger-garlic paste and tomato.",
                "Stir in chicken pieces, turmeric, cumin, and coriander; roast 6 minutes on medium flame.",
                "Add 1.5 cups warm water and potato; simmer covered 15 minutes until tender."
            )
        ),
        FoodDatabaseItem(
            id = "meat_boiled_eggs",
            nameEn = "Boiled Eggs (Dim Sheddho)",
            nameBn = "ডিম সেদ্ধ",
            nameHi = "उबले अंडे",
            category = "Meat and Fish",
            servingSizeG = 100.0,
            servingLabel = "2 large boiled eggs",
            calories = 155,
            protein = 12.6,
            carbohydrates = 1.1,
            fat = 10.6,
            fiber = 0.0,
            sugar = 1.1,
            cookingMethod = "Hard boiled",
            cuisine = "Indian"
        ),
        FoodDatabaseItem(
            id = "meat_egg_omelette",
            nameEn = "Masala Egg Omelette (2 Eggs)",
            nameBn = "ডিমের অমলেট",
            nameHi = "मसाला अंडा आमलेट",
            category = "Meat and Fish",
            servingSizeG = 115.0,
            servingLabel = "2-egg omelette",
            calories = 198,
            protein = 13.0,
            carbohydrates = 2.8,
            fat = 15.2,
            fiber = 0.5,
            sugar = 1.4,
            cookingMethod = "Pan-fried with onion, green chili, and coriander",
            cuisine = "Indian",
            prepTimeMinutes = 8,
            ingredients = listOf(
                "2 whole eggs, 2 tbsp chopped onion & tomato",
                "1 green chili, coriander, pinch of turmeric & pepper",
                "1 tsp oil or butter"
            ),
            recipeSteps = listOf(
                "Whisk eggs briskly with chopped onion, tomato, chili, coriander, turmeric, and salt.",
                "Pour into a lightly oiled hot skillet and cook 2 minutes per side until fluffy."
            )
        ),
        FoodDatabaseItem(
            id = "meat_grilled_chicken",
            nameEn = "Tandoori / Grilled Chicken Breast",
            nameBn = "তন্দুরি চিকেন",
            nameHi = "तंदूरी ग्रिल्ड चिकन",
            category = "Meat and Fish",
            servingSizeG = 150.0,
            servingLabel = "150g serving",
            calories = 240,
            protein = 36.5,
            carbohydrates = 3.2,
            fat = 8.4,
            fiber = 0.6,
            sugar = 1.2,
            cookingMethod = "Roasted in yogurt and tandoori spices",
            cuisine = "Indian",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "150g chicken breast scored lightly",
                "2 tbsp hung curd, 1 tsp ginger-garlic, 1 tsp tandoori masala, lemon juice",
                "1 tsp oil for searing"
            ),
            recipeSteps = listOf(
                "Coat chicken in spiced yogurt marinade for 20 minutes.",
                "Grill or pan-sear 5–6 minutes per side until cooked through and charred."
            )
        ),

        // ==================== VEGETABLES & SALADS ====================
        FoodDatabaseItem(
            id = "veg_mixed_sabzi",
            nameEn = "Mixed Vegetable Sabzi (Aloo Gobi Beans)",
            nameBn = "পাঁচমিশালি সবজি",
            nameHi = "मिक्स वेज सब्ज़ी",
            category = "Vegetables",
            servingSizeG = 150.0,
            servingLabel = "1 bowl (150g)",
            calories = 135,
            protein = 3.6,
            carbohydrates = 17.5,
            fat = 5.6,
            fiber = 4.8,
            sugar = 3.4,
            cookingMethod = "Stir-fried seasonal vegetables with minimal oil",
            cuisine = "Indian",
            prepTimeMinutes = 20,
            ingredients = listOf(
                "150g cauliflower, carrot, green beans & potato cubes",
                "1/2 tsp cumin seeds, turmeric, coriander powder, ginger",
                "1.5 tsp mustard/vegetable oil"
            ),
            recipeSteps = listOf(
                "Temper cumin and ginger in oil, then toss in chopped vegetables, turmeric, and salt.",
                "Cover and cook on low heat 12 minutes, stirring occasionally, until tender-crisp."
            )
        ),
        FoodDatabaseItem(
            id = "veg_green_salad",
            nameEn = "Fresh Cucumber, Tomato & Onion Salad",
            nameBn = "শসা-টমেটো সালাদ",
            nameHi = "खीरा-टमाटर सलाद",
            category = "Vegetables",
            servingSizeG = 150.0,
            servingLabel = "1 plate (150g)",
            calories = 42,
            protein = 1.6,
            carbohydrates = 8.8,
            fat = 0.3,
            fiber = 2.6,
            sugar = 4.5,
            cookingMethod = "Raw sliced vegetables with lemon juice",
            cuisine = "Indian"
        ),
        FoodDatabaseItem(
            id = "veg_palak_saag",
            nameEn = "Sautéed Spinach / Palong Shaak",
            nameBn = "পালং শাক ভাজা",
            nameHi = "पालक साग",
            category = "Vegetables",
            servingSizeG = 140.0,
            servingLabel = "1 bowl (140g)",
            calories = 95,
            protein = 4.2,
            carbohydrates = 9.0,
            fat = 4.8,
            fiber = 3.9,
            sugar = 1.1,
            cookingMethod = "Lightly sautéed leafy greens with garlic/panch phoron",
            cuisine = "Indian"
        ),

        // ==================== DAIRY PRODUCTS ====================
        FoodDatabaseItem(
            id = "dairy_toned_milk",
            nameEn = "Warm Toned Milk (Dudh)",
            nameBn = "গরুর দুধ",
            nameHi = "गाय का दूध",
            category = "Dairy Products",
            servingSizeG = 200.0,
            servingLabel = "1 glass (200ml)",
            calories = 120,
            protein = 6.6,
            carbohydrates = 9.8,
            fat = 6.0,
            fiber = 0.0,
            sugar = 9.8,
            cookingMethod = "Pasteurized boiled milk",
            cuisine = "Indian"
        ),
        FoodDatabaseItem(
            id = "dairy_plain_curd",
            nameEn = "Plain Homemade Curd / Tok Doi",
            nameBn = "টক দই",
            nameHi = "सादा दही",
            category = "Dairy Products",
            servingSizeG = 120.0,
            servingLabel = "1 katori (120g)",
            calories = 78,
            protein = 4.8,
            carbohydrates = 5.6,
            fat = 4.0,
            fiber = 0.0,
            sugar = 5.2,
            cookingMethod = "Cultured probiotic yogurt",
            cuisine = "Indian"
        ),
        FoodDatabaseItem(
            id = "dairy_raw_paneer",
            nameEn = "Fresh Paneer / Chhena (Cottage Cheese)",
            nameBn = "ছানা / পনির",
            nameHi = "ताज़ा पनीर / छेना",
            category = "Dairy Products",
            servingSizeG = 100.0,
            servingLabel = "100g cubes",
            calories = 265,
            protein = 18.3,
            carbohydrates = 3.4,
            fat = 20.8,
            fiber = 0.0,
            sugar = 2.1,
            cookingMethod = "Fresh milk curdled with lemon",
            cuisine = "Indian"
        ),

        // ==================== FRUITS ====================
        FoodDatabaseItem(
            id = "fruit_banana",
            nameEn = "Ripe Banana (Kola)",
            nameBn = "পাকা কলা",
            nameHi = "पका केला",
            category = "Fruits",
            servingSizeG = 118.0,
            servingLabel = "1 medium banana",
            calories = 105,
            protein = 1.3,
            carbohydrates = 27.0,
            fat = 0.4,
            fiber = 3.1,
            sugar = 14.4,
            cookingMethod = "Raw fresh fruit",
            cuisine = "Global"
        ),
        FoodDatabaseItem(
            id = "fruit_apple",
            nameEn = "Fresh Apple (Apel)",
            nameBn = "আপেল",
            nameHi = "सेब",
            category = "Fruits",
            servingSizeG = 150.0,
            servingLabel = "1 medium apple",
            calories = 78,
            protein = 0.4,
            carbohydrates = 20.6,
            fat = 0.2,
            fiber = 3.6,
            sugar = 15.6,
            cookingMethod = "Raw fresh fruit",
            cuisine = "Global"
        ),
        FoodDatabaseItem(
            id = "fruit_mango",
            nameEn = "Himsagar / Alphonso Mango (Aam)",
            nameBn = "হিমসাগর আম",
            nameHi = "पका आम",
            category = "Fruits",
            servingSizeG = 150.0,
            servingLabel = "1 cup sliced (150g)",
            calories = 99,
            protein = 1.4,
            carbohydrates = 24.7,
            fat = 0.6,
            fiber = 2.6,
            sugar = 22.5,
            cookingMethod = "Raw fresh fruit",
            cuisine = "Indian"
        ),

        // ==================== SNACKS & STREET FOOD ====================
        FoodDatabaseItem(
            id = "snack_steamed_momo",
            nameEn = "Steamed Veg & Chicken Street Momo with Soup",
            nameBn = "স্টিমড মোমো ও স্যুপ",
            nameHi = "स्टीम्ड मोमो और सूप",
            category = "Snacks",
            servingSizeG = 180.0,
            servingLabel = "6 pieces + clear soup",
            calories = 260,
            protein = 16.5,
            carbohydrates = 32.0,
            fat = 7.2,
            fiber = 1.8,
            sugar = 1.5,
            cookingMethod = "Steamed dumplings filled with minced chicken, cabbage, and herbs",
            cuisine = "Nepali"
        ),
        FoodDatabaseItem(
            id = "snack_jhalmuri",
            nameEn = "Kolkata Jhalmuri (Spiced Puffed Rice)",
            nameBn = "ঝালমুড়ি",
            nameHi = "झालमुड़ी (मुरमुरा चाट)",
            category = "Snacks",
            servingSizeG = 80.0,
            servingLabel = "1 paper cone (80g)",
            calories = 185,
            protein = 4.8,
            carbohydrates = 32.5,
            fat = 4.2,
            fiber = 2.6,
            sugar = 1.2,
            cookingMethod = "Puffed rice tossed with roasted chana, onion, cucumber, and mustard oil",
            cuisine = "Bengali",
            prepTimeMinutes = 5,
            ingredients = listOf(
                "40g crisp puffed rice (muri)",
                "1 tbsp roasted peanuts & black chana, 2 tbsp chopped onion-cucumber-tomato",
                "1 tsp mustard oil, roasted cumin-chili masala & lemon"
            ),
            recipeSteps = listOf(
                "Combine chopped onion, cucumber, tomato, green chili, roasted chana, and peanuts in a bowl.",
                "Add puffed rice, bhaja masala, salt, and 1 tsp mustard oil; shake briskly and serve immediately."
            )
        ),
        FoodDatabaseItem(
            id = "snack_samosa",
            nameEn = "Singara / Aloo Samosa",
            nameBn = "সিঙাড়া",
            nameHi = "समोसा",
            category = "Snacks",
            servingSizeG = 80.0,
            servingLabel = "1 medium piece (80g)",
            calories = 215,
            protein = 3.8,
            carbohydrates = 24.5,
            fat = 11.4,
            fiber = 2.1,
            sugar = 1.2,
            cookingMethod = "Deep-fried pastry stuffed with spiced potatoes and peanuts",
            cuisine = "Indian"
        ),

        // ==================== BEVERAGES ====================
        FoodDatabaseItem(
            id = "bev_masala_chai",
            nameEn = "Indian Milk Tea (Dudh Cha / Chai)",
            nameBn = "দুধ চা",
            nameHi = "मसाला चाय",
            category = "Beverages",
            servingSizeG = 150.0,
            servingLabel = "1 cup (150ml, 1 tsp sugar)",
            calories = 85,
            protein = 2.4,
            carbohydrates = 11.2,
            fat = 3.2,
            fiber = 0.0,
            sugar = 9.5,
            cookingMethod = "Brewed black tea with milk, ginger, and cardamom",
            cuisine = "Indian",
            prepTimeMinutes = 7,
            ingredients = listOf(
                "100ml water + 60ml milk",
                "1 tsp loose black tea leaves, 2 crushed cardamom pods, 1/2 inch ginger",
                "1 tsp sugar (optional)"
            ),
            recipeSteps = listOf(
                "Boil water with crushed ginger and cardamom for 2 minutes, then add tea leaves.",
                "Pour in milk and sugar, simmer 2 minutes until rich amber, and strain hot."
            )
        ),
        FoodDatabaseItem(
            id = "bev_green_tea",
            nameEn = "Darjeeling Green / Lemon Tea (Unsweetened)",
            nameBn = "গ্রিন টি / লেবু চা",
            nameHi = "ग्रीन टी / नींबू चाय",
            category = "Beverages",
            servingSizeG = 150.0,
            servingLabel = "1 cup (150ml)",
            calories = 4,
            protein = 0.1,
            carbohydrates = 0.8,
            fat = 0.0,
            fiber = 0.0,
            sugar = 0.0,
            cookingMethod = "Steeped tea leaves in hot water",
            cuisine = "Indian"
        ),

        // ==================== FAST FOOD & INTERNATIONAL ====================
        FoodDatabaseItem(
            id = "fast_egg_hakka_noodles",
            nameEn = "Egg Chicken Chowmein / Hakka Noodles",
            nameBn = "এগ চিকেন চাউমিন",
            nameHi = "एग चिकन चाउमीन / नूडल्स",
            category = "Fast Food",
            servingSizeG = 240.0,
            servingLabel = "1 plate (240g)",
            calories = 430,
            protein = 16.8,
            carbohydrates = 54.0,
            fat = 16.2,
            fiber = 3.2,
            sugar = 3.8,
            cookingMethod = "Wok-tossed noodles with egg, chicken, cabbage, and capsicum",
            cuisine = "Indo-Chinese"
        ),
        FoodDatabaseItem(
            id = "intl_grilled_salad_bowl",
            nameEn = "Mediterranean Chickpea & Paneer Quinoa Bowl",
            nameBn = "চিকপি ও পনির সালাদ বোল",
            nameHi = "चना और पनीर सलाद बाउल",
            category = "International Foods",
            servingSizeG = 260.0,
            servingLabel = "1 power bowl (260g)",
            calories = 325,
            protein = 17.5,
            carbohydrates = 36.0,
            fat = 12.0,
            fiber = 7.4,
            sugar = 3.2,
            cookingMethod = "Boiled chickpeas, grilled cottage cheese, greens, and olive oil lemon dressing",
            cuisine = "Mediterranean",
            prepTimeMinutes = 15,
            ingredients = listOf(
                "80g cooked quinoa, 60g boiled chickpeas, 60g grilled paneer",
                "60g cucumber, cherry tomatoes & spinach",
                "1 tsp olive oil, lemon juice, oregano & black pepper"
            ),
            recipeSteps = listOf(
                "Arrange cooked quinoa, baby spinach, cucumber, tomatoes, and boiled chickpeas in a bowl.",
                "Top with warm pan-seared paneer cubes and drizzle olive oil-lemon-oregano dressing."
            )
        )
    )

    fun search(
        query: String,
        category: String = "All",
        recipesOnly: Boolean = false
    ): List<FoodDatabaseItem> {
        val trimmed = query.trim().lowercase()
        return allFoods.filter { item ->
            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
            val matchesRecipe = !recipesOnly || item.hasRecipe
            val matchesQuery = trimmed.isEmpty() ||
                item.nameEn.lowercase().contains(trimmed) ||
                item.nameBn.lowercase().contains(trimmed) ||
                item.nameHi.lowercase().contains(trimmed) ||
                item.category.lowercase().contains(trimmed) ||
                item.cuisine.lowercase().contains(trimmed) ||
                item.ingredients.any { it.lowercase().contains(trimmed) }
            matchesCategory && matchesRecipe && matchesQuery
        }
    }

    /**
     * Maps any food item or diary entry to its matching photorealistic AI-generated food drawable.
     */
    fun foodImageResFor(foodName: String, category: String = ""): Int {
        val text = "$foodName $category".lowercase()
        return when {
            text.contains("fish") || text.contains("mach") || text.contains("মাছ") ||
                text.contains("ilish") || text.contains("ইলিশ") || text.contains("chingri") ||
                text.contains("চিংড়ি") || text.contains("prawn") || text.contains("katla") ||
                text.contains("কাতলা") || text.contains("mangsho") || text.contains("মাংস") ||
                text.contains("mutton") || text.contains("chicken curry") || text.contains("tikka") ||
                text.contains("sekuwa") || text.contains("choyela") || text.contains("egg curry") ||
                text.contains("ডিমের") || category.equals("Meat and Fish", ignoreCase = true) -> {
                com.example.R.drawable.img_food_bengali_fish_curry_1793802386226
            }
            text.contains("biryani") || text.contains("বিরিয়ানি") || text.contains("rice") ||
                text.contains("bhaat") || text.contains("bhat") || text.contains("ভাত") ||
                text.contains("khichuri") || text.contains("khichdi") || text.contains("খিচুড়ি") ||
                text.contains("roti") || text.contains("রুটি") || text.contains("chapati") ||
                text.contains("paratha") || text.contains("পরোটা") || text.contains("oats") ||
                text.contains("ওটস") || text.contains("bread") || text.contains("dosa") ||
                text.contains("idli") || text.contains("uttapam") || text.contains("pongal") ||
                text.contains("upma") || text.contains("appam") || text.contains("pesarattu") ||
                text.contains("dhido") || text.contains("poha") ||
                category.equals("Rice and Grains", ignoreCase = true) -> {
                com.example.R.drawable.img_food_biryani_rice_grains_1793802387127
            }
            text.contains("samosa") || text.contains("singara") || text.contains("সিঙাড়া") ||
                text.contains("luchi") || text.contains("লুচি") || text.contains("momo") ||
                text.contains("মোমো") || text.contains("thukpa") || text.contains("vada") ||
                text.contains("chatamari") || text.contains("sel roti") || text.contains("chowmein") ||
                text.contains("চাউমিন") || text.contains("noodle") || text.contains("roll") ||
                text.contains("রোল") || text.contains("phuchka") || text.contains("ফুচকা") ||
                text.contains("jhalmuri") || text.contains("ঝালমুড়ি") || text.contains("pakora") ||
                text.contains("চপ") || category.equals("Snacks", ignoreCase = true) ||
                category.equals("Fast Food", ignoreCase = true) -> {
                com.example.R.drawable.img_food_snacks_street_fastfood_1793802389454
            }
            text.contains("doi") || text.contains("দই") || text.contains("rosogolla") ||
                text.contains("রসগোল্লা") || text.contains("sandesh") || text.contains("সন্দেশ") ||
                text.contains("yomari") || text.contains("sweet") || text.contains("milk") ||
                text.contains("দুধ") || text.contains("curd") || text.contains("yogurt") ||
                text.contains("lassi") || text.contains("লস্যি") || text.contains("tea") ||
                text.contains("চা") || text.contains("coffee") || text.contains("কফি") ||
                text.contains("mango") || text.contains("আম") || text.contains("banana") ||
                text.contains("কলা") || text.contains("apple") || text.contains("আপেল") ||
                text.contains("papaya") || text.contains("পেঁপে") || text.contains("guava") ||
                text.contains("পেয়ারা") || text.contains("coconut") || text.contains("ডাব") ||
                category.equals("Dairy Products", ignoreCase = true) ||
                category.equals("Fruits", ignoreCase = true) ||
                category.equals("Beverages", ignoreCase = true) -> {
                com.example.R.drawable.img_food_sweets_dairy_fruits_1793802390207
            }
            text.contains("dal") || text.contains("ডাল") || text.contains("paneer") ||
                text.contains("পনির") || text.contains("shukto") || text.contains("শুক্তো") ||
                text.contains("alu") || text.contains("aloo") || text.contains("আলু") ||
                text.contains("begun") || text.contains("বেগুন") || text.contains("veg") ||
                text.contains("সবজি") || text.contains("rajma") || text.contains("chole") ||
                text.contains("tadka") || text.contains("তরকা") || text.contains("palak") ||
                text.contains("sambar") || text.contains("rasam") || text.contains("gundruk") ||
                text.contains("kwati") || text.contains("tarkari") ||
                category.equals("Vegetables", ignoreCase = true) ||
                category.equals("Indian Foods", ignoreCase = true) ||
                category.equals("South Indian Foods", ignoreCase = true) ||
                category.equals("Nepali Foods", ignoreCase = true) ||
                category.equals("Bengali Foods", ignoreCase = true) -> {
                com.example.R.drawable.img_food_dal_paneer_veg_1793802388466
            }
            else -> {
                com.example.R.drawable.img_hero_nutrition_bowl_1793693566593
            }
        }
    }
}
