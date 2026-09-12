import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider, useTheme } from "./ThemeContext";

function ThemeProbe() {
  const { theme, setTheme } = useTheme();

  return (
    <div>
      <span data-testid="theme-value">{theme}</span>
      <button type="button" onClick={() => setTheme("dark")}>
        使用深色
      </button>
    </div>
  );
}

beforeEach(() => {
  window.localStorage.clear();
  delete document.documentElement.dataset.theme;
  document.documentElement.style.colorScheme = "";
});

test("defaults to system theme and persists a manual selection", () => {
  render(
    <ThemeProvider>
      <ThemeProbe />
    </ThemeProvider>
  );

  expect(screen.getByTestId("theme-value")).toHaveTextContent("system");

  fireEvent.click(screen.getByRole("button", { name: "使用深色" }));

  expect(screen.getByTestId("theme-value")).toHaveTextContent("dark");
  expect(document.documentElement.dataset.theme).toBe("dark");
  expect(document.documentElement.style.colorScheme).toBe("dark");
  expect(window.localStorage.getItem("shopping-website-theme")).toBe("dark");
});

test("restores a valid stored theme", () => {
  window.localStorage.setItem("shopping-website-theme", "light");

  render(
    <ThemeProvider>
      <ThemeProbe />
    </ThemeProvider>
  );

  expect(screen.getByTestId("theme-value")).toHaveTextContent("light");
  expect(document.documentElement.dataset.theme).toBe("light");
});

test("falls back to system when local preference cannot be read", () => {
  const getItem = jest.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
    throw new Error("storage unavailable");
  });

  render(
    <ThemeProvider>
      <ThemeProbe />
    </ThemeProvider>
  );

  expect(screen.getByTestId("theme-value")).toHaveTextContent("system");
  expect(document.documentElement.dataset.theme).toBe("system");
  getItem.mockRestore();
});

test("keeps theme switching available when local preference cannot be written", () => {
  const setItem = jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
    throw new Error("storage unavailable");
  });

  render(
    <ThemeProvider>
      <ThemeProbe />
    </ThemeProvider>
  );

  fireEvent.click(screen.getByRole("button", { name: "使用深色" }));

  expect(screen.getByTestId("theme-value")).toHaveTextContent("dark");
  expect(document.documentElement.dataset.theme).toBe("dark");
  setItem.mockRestore();
});
