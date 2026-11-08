package heritage.africca.accel_connect.mock;

import heritage.africca.accel_connect.entity.Meal;
import heritage.africca.accel_connect.entity.Restaurant;
import heritage.africca.accel_connect.repository.MealRepository;
import heritage.africca.accel_connect.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Mock implements CommandLineRunner {

   private final RestaurantRepository restaurantRepository;
   private final MealRepository mealRepository;

   @Override
   public void run(String... args) throws Exception {

       if (restaurantRepository.count() == 0) {

           // 1️⃣ Créer 4 restaurants
           List<Restaurant> restaurants = new ArrayList<>();
           for (int i = 0; i < 3; i++) {
               Restaurant r = new Restaurant();
               r.setRestaurantName("Restaurant_" + (i + 1));
               r.setAddress("Address_" + (i + 1));
               r.setDeliveryFee(500.0);
               r.setContact("77100101" + i);
               restaurants.add(r);
           }

           // 2️⃣ Créer 10 plats
           List<Meal> meals = new ArrayList<>();
           for (int i = 0; i < 10; i++) {
               Meal meal = new Meal();
               meal.setMealName("Meal_" + (i + 1));
               meal.setUnitPrice(1000.0 + i * 500);
               meals.add(meal);
           }

           // 3️⃣ Sauvegarder d'abord les meals
           mealRepository.saveAll(meals);

           // 4️⃣ Associer les plats aux restaurants (ex : chaque resto a 5 plats)
           for (int i = 0; i < restaurants.size(); i++) {
               Restaurant r = restaurants.get(i);
               List<Meal> subset = meals.subList(0, 5); // tu peux varier selon besoin
               r.setMeals(subset);
           }

           // 5️⃣ Sauvegarder les restaurants (sans erreur de référence)
           restaurantRepository.saveAll(restaurants);

           System.out.println("✅ Données mock insérées avec succès !");
       }
   }
}
