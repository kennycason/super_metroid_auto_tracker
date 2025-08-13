export interface ItemConfig {
  id: string;
  name: string;
  sprite: string;
  enabled: boolean;
  row: number;
  col: number;
  category: 'major' | 'expansion' | 'beam';
}

export interface BossConfig {
  id: string;
  name: string;
  sprite: string;
  enabled: boolean;
  row: number;
  col: number;
  category: 'major' | 'mini';
}

export interface LayoutSection {
  type: 'items' | 'bosses' | 'stats' | 'timer' | 'splits' | 'location';
  enabled: boolean;
  position: {
    row: number;
    col: number;
    width?: number;
    height?: number;
  };
  config?: Record<string, any>;
}

export interface TrackerConfig {
  id: string;
  name: string;
  description: string;
  items: ItemConfig[];
  bosses: BossConfig[];
  layout: LayoutSection[];
  settings: {
    showStats: boolean;
    showTimer: boolean;
    showSplits: boolean;
    showLocation: boolean;
    autoSplit: boolean;
    fullscreenAvailable: boolean;
    theme: 'dark' | 'light';
  };
} 