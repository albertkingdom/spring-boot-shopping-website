import { render, screen } from '@testing-library/react';
import App from './App';

test('renders the public shop navigation', () => {
  render(<App />);
  const linkElement = screen.getByText("Ecommerce");
  expect(linkElement).toBeInTheDocument();
  expect(screen.getByRole("button", { name: /顏色模式：/ })).toBeInTheDocument();
});
