import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import BackofficeShell from "./BackofficeShell";

test("renders grouped admin navigation and active breadcrumb", () => {
  render(
    <MemoryRouter initialEntries={["/admin/sellers"]}>
      <BackofficeShell workspace="admin" userRole={["ROLE_ADMIN"]}>
        <h1>商家權限內容</h1>
      </BackofficeShell>
    </MemoryRouter>
  );

  expect(screen.getByRole("heading", { name: "工作台" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "營運" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "管理" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "商家權限" })).toHaveAttribute(
    "aria-current",
    "page"
  );
  expect(screen.getByLabelText("breadcrumb")).toHaveTextContent(
    "後台 / 平台管理 / 商家權限"
  );
});

test("keeps seller navigation scoped to seller modules", () => {
  render(
    <MemoryRouter initialEntries={["/seller"]}>
      <BackofficeShell workspace="seller" userRole={["ROLE_SELLER"]}>
        <h1>商家中心內容</h1>
      </BackofficeShell>
    </MemoryRouter>
  );

  expect(screen.getByText("只顯示本店商品與訂單。")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "我的商品" })).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "商家權限" })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "全站訂單" })).not.toBeInTheDocument();
});
