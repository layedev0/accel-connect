package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.MealDTO;

import java.util.List;

public interface MealService {
    List<MealDTO> getAllMeals();
    MealDTO getMealById(Long id);
    MealDTO createMeal(MealDTO dto);
    MealDTO updateMeal(Long id, MealDTO dto);
    void deleteMeal(Long id);
}