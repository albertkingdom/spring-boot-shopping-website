import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import OrderListPageForSeller from "./OrderListPageForSeller";

describe("OrderListPageForSeller", () => {
  const page = (content, totalPages = 1) => ({
    ok: true,
    json: async () => ({ content, totalPages }),
  });

  test("shows an empty state and next step when the seller has no orders", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn().mockResolvedValue(page([]));

    render(
      <MemoryRouter>
        <OrderListPageForSeller refreshAccessToken={refreshAccessToken} />
      </MemoryRouter>
    );

    expect(await screen.findByRole("status")).toHaveTextContent("目前沒有訂單");
    expect(screen.getByRole("button", { name: "查看我的商品" })).toBeInTheDocument();
  });

  test("does not turn a missing createdAt into a 1970 date", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn().mockResolvedValue(page([
      {
        id: 3,
        buyerEmail: "buyer@example.com",
        sellerSubtotal: "12.34",
        createdAt: null,
      },
    ]));

    render(
      <MemoryRouter>
        <OrderListPageForSeller refreshAccessToken={refreshAccessToken} />
      </MemoryRouter>
    );

    expect(await screen.findByText("—")).toBeInTheDocument();
    expect(screen.queryByText(/1970/)).not.toBeInTheDocument();
  });
});
