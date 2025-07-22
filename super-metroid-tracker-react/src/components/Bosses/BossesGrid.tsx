import React from 'react';
import { Boss } from './Boss';
import { useSuperMetroid } from '../../context/SuperMetroidContext';
import './BossesGrid.css';

export const BossesGrid: React.FC = () => {
  const { config } = useSuperMetroid();
  
  // Get enabled bosses and sort by row/col
  const enabledBosses = config.bosses
    .filter(boss => boss.enabled)
    .sort((a, b) => {
      if (a.row !== b.row) return a.row - b.row;
      return a.col - b.col;
    });

  // Group bosses by row
  const bossesByRow = enabledBosses.reduce((acc, boss) => {
    if (!acc[boss.row]) acc[boss.row] = [];
    acc[boss.row].push(boss);
    return acc;
  }, {} as Record<number, typeof enabledBosses>);

  // Sort rows and ensure each row is sorted by column
  const rows = Object.keys(bossesByRow)
    .map(Number)
    .sort((a, b) => a - b)
    .map(rowNum => ({
      row: rowNum,
      bosses: bossesByRow[rowNum].sort((a, b) => a.col - b.col)
    }));

  return (
    <div className="bosses-grid">
      <div className="bosses-header">Bosses</div>
      <div className="bosses-container">
        {rows.map(({ row, bosses }) => (
          <div key={row} className="bosses-row">
            {bosses.map(boss => (
              <Boss key={boss.id} config={boss} />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}; 