package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.MealDTO;
import heritage.africca.accel_connect.entity.Meal;
import heritage.africca.accel_connect.repository.MealRepository;
import heritage.africca.accel_connect.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;

    @Transactional(readOnly = true)
    @Override
    public List<MealDTO> getAllMeals() {
        return mealRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public MealDTO getMealById(Long id) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plat non trouvé: " + id));
        return convertToDTO(meal);
    }

    @Transactional
    @Override
    public MealDTO createMeal(MealDTO dto) {
        Meal meal = new Meal();
        meal.setMealName(dto.getMealName());
        meal.setUnitPrice(dto.getUnitPrice());
        meal.setImageUrl(dto.getImageUrl());

        Meal saved = mealRepository.save(meal);
        log.info("✅ Plat créé: {}", saved.getMealName());

        return convertToDTO(saved);
    }

    @Transactional
    @Override
    public MealDTO updateMeal(Long id, MealDTO dto) {
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plat non trouvé: " + id));

        if (dto.getMealName() != null) {
            meal.setMealName(dto.getMealName());
        }
        if (dto.getUnitPrice() != null) {
            meal.setUnitPrice(dto.getUnitPrice());
        }
        if (dto.getImageUrl() != null) {
            meal.setImageUrl(dto.getImageUrl());
        }

        Meal updated = mealRepository.save(meal);
        log.info("✅ Plat mis à jour: {}", updated.getMealName());

        return convertToDTO(updated);
    }

    @Transactional
    @Override
    public void deleteMeal(Long id) {
        if (!mealRepository.existsById(id)) {
            throw new RuntimeException("Plat non trouvé: " + id);
        }
        mealRepository.deleteById(id);
        log.info("✅ Plat supprimé: {}", id);
    }

    private MealDTO convertToDTO(Meal meal) {
        MealDTO dto = new MealDTO();
        dto.setId(meal.getId());
        dto.setMealName(meal.getMealName());
        dto.setUnitPrice(meal.getUnitPrice());
        dto.setImageUrl(meal.getImageUrl());
        return dto;
    }
}