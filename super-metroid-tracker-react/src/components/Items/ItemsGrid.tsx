import React from 'react';
import { Item } from './Item';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './ItemsGrid.css';

export const ItemsGrid: React.FC = () => {
  const { config } = useSuperMetroid();
  
  // Get enabled items and sort by row/col
  const enabledItems = config.items
    .filter(item => item.enabled)
    .sort((a, b) => {
      if (a.row !== b.row) return a.row - b.row;
      return a.col - b.col;
    });

  // Group items by row
  const itemsByRow = enabledItems.reduce((acc, item) => {
    if (!acc[item.row]) acc[item.row] = [];
    acc[item.row].push(item);
    return acc;
  }, {} as Record<number, typeof enabledItems>);

  // Sort rows and ensure each row is sorted by column
  const rows = Object.keys(itemsByRow)
    .map(Number)
    .sort((a, b) => a - b)
    .map(rowNum => ({
      row: rowNum,
      items: itemsByRow[rowNum].sort((a, b) => a.col - b.col)
    }));

  return (
    <div className="items-grid">
      <div className="items-header">Items</div>
      <div className="items-container">
        {rows.map(({ row, items }) => (
          <div key={row} className="items-row">
            {items.map(item => (
              <Item 
                key={item.id} 
                config={item} 
                showCount={item.category === 'expansion'}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}; 