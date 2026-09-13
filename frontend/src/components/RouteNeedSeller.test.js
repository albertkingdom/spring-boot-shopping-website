import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import RouteNeedSeller from "./RouteNeedSeller";

test("allows a seller into the seller center", () => {
  render(
    <MemoryRouter>
      <RouteNeedSeller userName="seller@example.com" userRole={["ROLE_SELLER"]} redirectTo="/login">
        <div>商家內容</div>
      </RouteNeedSeller>
    </MemoryRouter>
  );

  expect(screen.getByText("商家內容")).toBeInTheDocument();
});

test("rejects a signed-in user without seller role", () => {
  render(
    <MemoryRouter>
      <RouteNeedSeller userName="buyer@example.com" userRole={["ROLE_USER"]} redirectTo="/login">
        <div>商家內容</div>
      </RouteNeedSeller>
    </MemoryRouter>
  );

  expect(screen.getByText("你沒有商家中心的權限。")).toBeInTheDocument();
});
