export interface MenuItem {
  id: string;
  name: string;
  price: number;
  emoji: string;
  quantity: number;
  gradientFrom: string;
  gradientTo: string;
  hoverBorder: string;
}

export interface Restaurant {
  id: string;
  name: string;
  subtitle: string;
  icon?: string;
  gradientFrom: string;
  gradientTo: string;
  items: MenuItem[];
}

export interface Stat {
  id: string;
  label: string;
  value: string;
  icon: string;
  gradientFrom: string;
  gradientTo: string;
}

export interface CartItem {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  emoji: string;
  gradientFrom: string;
  gradientTo: string;
  restaurantName: string;
  restaurantGradientFrom: string;
  restaurantGradientTo: string;
}
