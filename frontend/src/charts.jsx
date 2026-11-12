import React, { useState } from "react";

/**
 * Reusable, pure-SVG chart components for the FlowEdu dashboard.
 * Zero dependencies beyond React — designed to match the existing design tokens.
 */

const CHART_COLORS = {
  mint: "#6dcfb0",
  sky: "#6aafe8",
  peach: "#f0a87a",
  lavender: "#a88ed0",
  rose: "#e08a9e",
  ink: "#292524",
  muted: "#a8a29e",
  success: "#16a34a",
  error: "#dc2626",
  hairline: "#e7e5e4",
  surface: "#f5f5f5",
};

/* ─────────────────── BAR CHART ─────────────────── */
export function BarChart({ data = [], height = 200, barColor = CHART_COLORS.mint, label = "" }) {
  const [hovered, setHovered] = useState(null);
  const max = Math.max(...data.map((d) => d.value), 1);
  const barCount = data.length || 1;
  const gap = 8;
  const barWidth = `calc((100% - ${(barCount - 1) * gap}px) / ${barCount})`;

  return (
    <div className="chart-container">
      {label && <div className="chart-label">{label}</div>}
      <div className="bar-chart" style={{ height, position: "relative" }}>
        {/* Y-axis guide lines */}
        {[0, 0.25, 0.5, 0.75, 1].map((pct) => (
          <div
            key={pct}
            className="bar-chart-guide"
            style={{ bottom: `${pct * 100}%` }}
          />
        ))}
        <div className="bar-chart-bars" style={{ display: "flex", alignItems: "flex-end", gap, height: "100%", position: "relative", zIndex: 1 }}>
          {data.map((d, i) => {
            const pct = max > 0 ? (d.value / max) * 100 : 0;
            return (
              <div
                key={i}
                className="bar-chart-col"
                style={{ width: barWidth, display: "flex", flexDirection: "column", alignItems: "center", height: "100%", justifyContent: "flex-end" }}
                onMouseEnter={() => setHovered(i)}
                onMouseLeave={() => setHovered(null)}
              >
                {hovered === i && (
                  <div className="bar-tooltip">{d.value}</div>
                )}
                <div
                  className="bar-fill"
                  style={{
                    width: "100%",
                    height: `${pct}%`,
                    minHeight: d.value > 0 ? 4 : 0,
                    background: `linear-gradient(180deg, ${barColor}, ${barColor}88)`,
                    borderRadius: "6px 6px 2px 2px",
                    transition: "height 0.6s cubic-bezier(0.34, 1.56, 0.64, 1)",
                    opacity: hovered !== null && hovered !== i ? 0.4 : 1,
                  }}
                />
              </div>
            );
          })}
        </div>
      </div>
      {/* X-axis labels */}
      <div className="bar-chart-labels" style={{ display: "flex", gap, marginTop: 8 }}>
        {data.map((d, i) => (
          <div key={i} style={{ width: barWidth, textAlign: "center", fontSize: 11, color: CHART_COLORS.muted, fontWeight: hovered === i ? 600 : 400 }}>
            {d.label}
          </div>
        ))}
      </div>
    </div>
  );
}

/* ─────────────────── DONUT CHART ─────────────────── */
export function DonutChart({ data = [], size = 180, label = "" }) {
  const [hovered, setHovered] = useState(null);
  const total = data.reduce((s, d) => s + d.value, 0) || 1;
  const colors = [CHART_COLORS.mint, CHART_COLORS.sky, CHART_COLORS.peach, CHART_COLORS.lavender, CHART_COLORS.rose];
  const radius = 70;
  const cx = 90, cy = 90;
  const strokeWidth = 24;

  // Build arcs
  let cumAngle = -90; // start at top
  const arcs = data.map((d, i) => {
    const angle = (d.value / total) * 360;
    const startAngle = cumAngle;
    cumAngle += angle;
    return { ...d, angle, startAngle, endAngle: cumAngle, color: d.color || colors[i % colors.length] };
  });

  function polarToCart(angleDeg, r) {
    const rad = (angleDeg * Math.PI) / 180;
    return [cx + r * Math.cos(rad), cy + r * Math.sin(rad)];
  }

  function arcPath(startAngle, endAngle) {
    const [x1, y1] = polarToCart(startAngle, radius);
    const [x2, y2] = polarToCart(endAngle, radius);
    const largeArc = endAngle - startAngle > 180 ? 1 : 0;
    return `M ${x1} ${y1} A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2}`;
  }

  return (
    <div className="chart-container">
      {label && <div className="chart-label">{label}</div>}
      <div style={{ display: "flex", alignItems: "center", gap: 24, flexWrap: "wrap" }}>
        <svg width={size} height={size} viewBox="0 0 180 180" style={{ overflow: "visible" }}>
          {total > 0 && arcs.map((arc, i) => (
            <path
              key={i}
              d={arcPath(arc.startAngle, arc.endAngle - 0.5)}
              fill="none"
              stroke={arc.color}
              strokeWidth={hovered === i ? strokeWidth + 6 : strokeWidth}
              strokeLinecap="round"
              style={{
                transition: "stroke-width 0.25s ease, opacity 0.25s ease",
                opacity: hovered !== null && hovered !== i ? 0.35 : 1,
                cursor: "pointer",
                filter: hovered === i ? `drop-shadow(0 2px 8px ${arc.color}66)` : "none",
              }}
              onMouseEnter={() => setHovered(i)}
              onMouseLeave={() => setHovered(null)}
            />
          ))}
          {/* Center text */}
          <text x={cx} y={cy - 6} textAnchor="middle" fill={CHART_COLORS.ink} fontSize="28" fontWeight="300" fontFamily="'EB Garamond', serif">
            {total}
          </text>
          <text x={cx} y={cy + 14} textAnchor="middle" fill={CHART_COLORS.muted} fontSize="10" fontWeight="600" letterSpacing="0.8" style={{ textTransform: "uppercase" }}>
            TOTAL
          </text>
        </svg>
        {/* Legend */}
        <div className="donut-legend">
          {arcs.map((arc, i) => (
            <div
              key={i}
              className="donut-legend-item"
              onMouseEnter={() => setHovered(i)}
              onMouseLeave={() => setHovered(null)}
              style={{ opacity: hovered !== null && hovered !== i ? 0.5 : 1 }}
            >
              <span className="donut-legend-dot" style={{ background: arc.color }} />
              <span className="donut-legend-label">{arc.label}</span>
              <span className="donut-legend-value">{arc.value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ─────────────────── STACKED AREA / LINE CHART ─────────────────── */
export function AreaChart({ data = [], height = 200, label = "" }) {
  const [hovered, setHovered] = useState(null);
  const maxVal = Math.max(...data.map((d) => (d.success || 0) + (d.failed || 0)), 1);
  const w = 400, h = 180;
  const pad = { top: 10, right: 10, bottom: 0, left: 10 };
  const innerW = w - pad.left - pad.right;
  const innerH = h - pad.top - pad.bottom;
  const n = data.length || 1;
  const stepX = innerW / Math.max(n - 1, 1);

  function buildPath(key) {
    return data.map((d, i) => {
      const x = pad.left + i * stepX;
      const y = pad.top + innerH - ((d[key] || 0) / maxVal) * innerH;
      return `${i === 0 ? "M" : "L"} ${x} ${y}`;
    }).join(" ");
  }

  function buildAreaPath(key) {
    const linePath = buildPath(key);
    const lastX = pad.left + (n - 1) * stepX;
    const firstX = pad.left;
    const baseline = pad.top + innerH;
    return `${linePath} L ${lastX} ${baseline} L ${firstX} ${baseline} Z`;
  }

  return (
    <div className="chart-container">
      {label && <div className="chart-label">{label}</div>}
      <svg width="100%" height={height} viewBox={`0 0 ${w} ${h + 30}`} preserveAspectRatio="none" style={{ overflow: "visible" }}>
        {/* Grid lines */}
        {[0, 0.5, 1].map((pct) => (
          <line
            key={pct}
            x1={pad.left}
            y1={pad.top + innerH * (1 - pct)}
            x2={pad.left + innerW}
            y2={pad.top + innerH * (1 - pct)}
            stroke={CHART_COLORS.hairline}
            strokeWidth="1"
          />
        ))}

        {/* Success area */}
        <path d={buildAreaPath("success")} fill={`${CHART_COLORS.mint}25`} />
        <path d={buildPath("success")} fill="none" stroke={CHART_COLORS.mint} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />

        {/* Failed area */}
        <path d={buildAreaPath("failed")} fill={`${CHART_COLORS.rose}25`} />
        <path d={buildPath("failed")} fill="none" stroke={CHART_COLORS.rose} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />

        {/* Data points & hover zones */}
        {data.map((d, i) => {
          const x = pad.left + i * stepX;
          const yS = pad.top + innerH - ((d.success || 0) / maxVal) * innerH;
          const yF = pad.top + innerH - ((d.failed || 0) / maxVal) * innerH;
          return (
            <g key={i}
              onMouseEnter={() => setHovered(i)}
              onMouseLeave={() => setHovered(null)}
            >
              {/* Invisible hover area */}
              <rect x={x - stepX / 2} y={0} width={stepX} height={h + 30} fill="transparent" />
              {/* Hover line */}
              {hovered === i && (
                <line x1={x} y1={pad.top} x2={x} y2={pad.top + innerH} stroke={CHART_COLORS.muted} strokeWidth="1" strokeDasharray="4 3" />
              )}
              {/* Success dot */}
              <circle cx={x} cy={yS} r={hovered === i ? 5 : 3} fill={CHART_COLORS.mint} stroke="#fff" strokeWidth="2" style={{ transition: "r 0.2s ease" }} />
              {/* Failed dot */}
              <circle cx={x} cy={yF} r={hovered === i ? 5 : 3} fill={CHART_COLORS.rose} stroke="#fff" strokeWidth="2" style={{ transition: "r 0.2s ease" }} />
              {/* Tooltip */}
              {hovered === i && (
                <g>
                  <rect x={x - 40} y={Math.min(yS, yF) - 36} width="80" height="28" rx="6" fill={CHART_COLORS.ink} />
                  <text x={x} y={Math.min(yS, yF) - 18} textAnchor="middle" fill="#fff" fontSize="11" fontWeight="500">
                    ✓{d.success || 0}  ✕{d.failed || 0}
                  </text>
                </g>
              )}
              {/* X label */}
              <text x={x} y={h + 18} textAnchor="middle" fill={CHART_COLORS.muted} fontSize="11" fontWeight={hovered === i ? 600 : 400}>
                {d.label}
              </text>
            </g>
          );
        })}
      </svg>
      {/* Legend */}
      <div className="area-legend">
        <span className="area-legend-item"><span className="donut-legend-dot" style={{ background: CHART_COLORS.mint }} /> Success</span>
        <span className="area-legend-item"><span className="donut-legend-dot" style={{ background: CHART_COLORS.rose }} /> Failed</span>
      </div>
    </div>
  );
}
