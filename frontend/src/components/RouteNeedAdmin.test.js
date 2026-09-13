import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import RouteNeedAdmin from "./RouteNeedAdmin";

test("allows a platform admin into the platform console", () => {
  render(
    <MemoryRouter>
      <RouteNeedAdmin userName="admin@example.com" userRole={["ROLE_ADMIN"]} redirectTo="/login">
        <div>平台內容</div>
      </RouteNeedAdmin>
    </MemoryRouter>
  );

  expect(screen.getByText("平台內容")).toBeInTheDocument();
});

test("rejects a seller without the platform admin role", () => {
  render(
    <MemoryRouter>
      <RouteNeedAdmin userName="seller@example.com" userRole={["ROLE_SELLER"]} redirectTo="/login">
        <div>平台內容</div>
      </RouteNeedAdmin>
    </MemoryRouter>
  );

  expect(screen.getByText("You don't have permission in admin page.")).toBeInTheDocument();
});
