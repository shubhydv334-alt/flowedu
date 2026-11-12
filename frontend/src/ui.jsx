import React, { useState } from "react";
import "./tokens.css";
import "./components.css";

const cx = (...c) => c.filter(Boolean).join(" ");

/** Variants: "primary" | "outline" | "text". Ink pill is the only CTA color. */
export function Button({ variant = "primary", className, children, ...props }) {
  return (
    <button className={cx("btn", `btn-${variant}`, className)} {...props}>
      {children}
    </button>
  );
}

export function Card({ dark, hover, className, children, ...props }) {
  return (
    <div className={cx("card", dark && "card-dark", hover && "card-hover", className)} {...props}>
      {children}
    </div>
  );
}

/** tone: "default" | "success" | "error" */
export function Badge({ tone = "default", children }) {
  return <span className={cx("badge", tone !== "default" && `badge-${tone}`)}>{children}</span>;
}

export function Input({ label, error, id, ...props }) {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, "-") : undefined);
  return (
    <div className="field">
      {label && <label htmlFor={inputId}>{label}</label>}
      <input id={inputId} className={cx("input", error && "has-error")} {...props} />
      {error && <span className="field-error">{error}</span>}
    </div>
  );
}

/**
 * columns: [{ key, header, render?(row), strong? }]
 * rows: array of objects. Wrap in <Card style={{padding:0}}> for the card look.
 */
export function Table({ columns, rows, rowKey = "id" }) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>{columns.map((c) => <th key={c.key}>{c.header}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={row[rowKey] ?? i}>
              {columns.map((c) => (
                <td key={c.key} className={c.strong ? "strong" : undefined}>
                  {c.render ? c.render(row) : row[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** items: [{ label, href, roles? }]; filter by role before passing in. */
export function TopNav({ brand = "FlowEdu", items = [], activePath, actions, renderLink }) {
  const [open, setOpen] = useState(false);
  return (
    <header className="topnav" style={{ position: "relative" }}>
      <div className="topnav-inner">
        <a className="topnav-brand" href="/">{brand}</a>
        <nav className={cx("topnav-links", open && "open")}>
          {items.map((it) => {
            const cls = cx("topnav-link", activePath === it.href && "active");
            return renderLink ? renderLink(it, cls) : (
              <a key={it.href} href={it.href} className={cls}>{it.label}</a>
            );
          })}
        </nav>
        <div className="topnav-actions">
          {actions}
          <button className="topnav-burger" aria-label="Menu" onClick={() => setOpen(!open)}>☰</button>
        </div>
      </div>
    </header>
  );
}

/** Decorative blur orb. Place inside an element with className="orb-host". */
export function GradientOrb({ color = "mint", size = 320, top, left, right, bottom }) {
  return (
    <div
      className={cx("orb", `orb-${color}`)}
      style={{ width: size, height: size, top, left, right, bottom }}
      aria-hidden="true"
    />
  );
}
