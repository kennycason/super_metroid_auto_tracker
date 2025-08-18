import React, { useState } from 'react';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './ItemVisibilitySelector.css';

interface ItemGroup {
  name: string;
  items: Array<{ id: string; name: string }>;
}

export const ItemVisibilitySelector: React.FC = () => {
  const { 
    setItemVisibility, 
    setAllItemsVisibility,
    isItemVisible 
  } = useSuperMetroid();

  const [isExpanded, setIsExpanded] = useState(false);

  // Define item groups for better organization
  const itemGroups: ItemGroup[] = [
    {
      name: 'Quantity Items',
      items: [
        { id: 'energy_tank', name: 'Energy Tanks' },
        { id: 'reserve_tank', name: 'Reserve Tanks' },
        { id: 'missile_tank', name: 'Missiles' },
        { id: 'super_tank', name: 'Super Missiles' },
        { id: 'power_bomb_tank', name: 'Power Bombs' },
      ]
    },
    {
      name: 'Power-ups',
      items: [
        { id: 'morph', name: 'Morph Ball' },
        { id: 'bombs', name: 'Bombs' },
        { id: 'charge', name: 'Charge Beam' },
        { id: 'spazer', name: 'Spazer Beam' },
        { id: 'varia', name: 'Varia Suit' },
        { id: 'hi_jump', name: 'Hi-Jump Boots' },
        { id: 'speed_booster', name: 'Speed Booster' },
        { id: 'wave', name: 'Wave Beam' },
        { id: 'ice', name: 'Ice Beam' },
        { id: 'grapple', name: 'Grappling Beam' },
        { id: 'x_ray', name: 'X-Ray Scope' },
        { id: 'plasma', name: 'Plasma Beam' },
        { id: 'gravity', name: 'Gravity Suit' },
        { id: 'space_jump', name: 'Space Jump' },
        { id: 'spring', name: 'Spring Ball' },
        { id: 'screw_attack', name: 'Screw Attack' },
      ]
    },
    {
      name: 'Bosses',
      items: [
        { id: 'bomb_torizo', name: 'B.Torizo' },
        { id: 'spore_spawn', name: 'Spore' },
        { id: 'kraid', name: 'Kraid' },
        { id: 'crocomire', name: 'Crocomire' },
        { id: 'phantoon', name: 'Phantoon' },
        { id: 'botwoon', name: 'Botwoon' },
        { id: 'draygon', name: 'Draygon' },
        { id: 'golden_torizo', name: 'G.Torizo' },
        { id: 'ridley', name: 'Ridley' },
        { id: 'mother_brain_1', name: 'MB1' },
        { id: 'mother_brain_2', name: 'MB2' },
        { id: 'samus_ship', name: 'Ship' },
      ]
    }
  ];

  const allItemIds = itemGroups.flatMap(group => group.items.map(item => item.id));
  const visibleCount = allItemIds.filter(id => isItemVisible(id)).length;
  const totalCount = allItemIds.length;

  const handleToggleAll = () => {
    const allVisible = visibleCount === totalCount;
    setAllItemsVisibility(!allVisible);
  };

  const handleItemToggle = (itemId: string) => {
    setItemVisibility(itemId, !isItemVisible(itemId));
  };

  return (
    <div className="item-visibility-selector">
      <div className="selector-header">
        <button 
          className="selector-toggle"
          onClick={() => setIsExpanded(!isExpanded)}
        >
          <span className="toggle-icon">{isExpanded ? '▼' : '▶'}</span>
          Item Visibility ({visibleCount}/{totalCount})
        </button>
        <button 
          className="toggle-all-btn"
          onClick={handleToggleAll}
          title={visibleCount === totalCount ? 'Hide All' : 'Show All'}
        >
          {visibleCount === totalCount ? 'Hide All' : 'Show All'}
        </button>
      </div>

      {isExpanded && (
        <div className="selector-content">
          {itemGroups.map((group) => (
            <div key={group.name} className="item-group">
              <div className="group-header">
                <span className="group-name">{group.name}</span>
                <div className="group-controls">
                  <button
                    className="group-btn"
                    onClick={() => {
                      const allVisible = group.items.every(item => isItemVisible(item.id));
                      group.items.forEach(item => {
                        setItemVisibility(item.id, !allVisible);
                      });
                    }}
                  >
                    {group.items.every(item => isItemVisible(item.id)) ? 'Hide' : 'Show'} All
                  </button>
                </div>
              </div>
              <div className="item-list">
                {group.items.map((item) => (
                  <label key={item.id} className="item-checkbox">
                    <input
                      type="checkbox"
                      checked={isItemVisible(item.id)}
                      onChange={() => handleItemToggle(item.id)}
                    />
                    <span className="checkbox-label">{item.name}</span>
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
