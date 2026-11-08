import { Pipe, PipeTransform } from '@angular/core';

/**
 * 🔧 Pipe pour obtenir des valeurs uniques d'un tableau d'objets
 * Usage: array | unique: 'propertyName'
 */
@Pipe({
  name: 'unique',
  standalone: true,
})
export class UniquePipe implements PipeTransform {
  transform<T>(array: T[], property: keyof T): T[] {
    if (!array || !Array.isArray(array)) {
      return [];
    }

    const seen = new Set<any>();
    return array.filter((item) => {
      const value = item[property];
      if (seen.has(value)) {
        return false;
      }
      seen.add(value);
      return true;
    });
  }
}
