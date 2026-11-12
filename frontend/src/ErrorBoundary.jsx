import React from "react";
import { Button } from "./ui";

/** Catches render errors so one broken page shows a message instead of a blank screen. */
export default class ErrorBoundary extends React.Component {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error, info) {
    console.error("Unhandled UI error", error, info?.componentStack);
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <main className="container page" role="alert">
        <h1>Something went wrong</h1>
        <p className="muted">The page hit an unexpected error. Reloading usually fixes it.</p>
        <Button onClick={() => window.location.reload()}>Reload</Button>
      </main>
    );
  }
}
