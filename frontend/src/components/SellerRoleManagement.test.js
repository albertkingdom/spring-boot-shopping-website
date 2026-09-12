import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import SellerRoleManagement from "./SellerRoleManagement";

describe("SellerRoleManagement", () => {
  const user = {
    id: 1,
    email: "buyer@example.com",
    name: "Buyer",
    roles: ["ROLE_USER"],
  };

  test("requires confirmation before changing a seller role", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn().mockResolvedValue({ ok: true, json: async () => [user] });

    render(
      <MemoryRouter>
        <SellerRoleManagement refreshAccessToken={refreshAccessToken} />
      </MemoryRouter>
    );

    expect(await screen.findByText("buyer@example.com")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "授予商家" }));

    expect(screen.getByRole("dialog")).toHaveTextContent("確認要對 buyer@example.com 授予商家角色嗎？");
    expect(global.fetch).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole("button", { name: "取消" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  test("links to platform products when revoking a seller is blocked by owned products", async () => {
    const seller = { ...user, roles: ["ROLE_USER", "ROLE_SELLER"] };
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => [seller] })
      .mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ message: "seller owns products" }),
      });

    render(
      <MemoryRouter>
        <SellerRoleManagement refreshAccessToken={refreshAccessToken} />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole("button", { name: "撤銷商家" }));
    fireEvent.click(screen.getByRole("button", { name: "確認變更" }));

    const productsLink = await screen.findByRole("link", { name: "前往全站商品" });
    expect(productsLink).toHaveAttribute("href", "/admin/products");
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));
  });
});
