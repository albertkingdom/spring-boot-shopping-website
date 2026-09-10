import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ProductPageForSeller from "./ProductPageForSeller";

describe("ProductPageForSeller", () => {
  const page = (content, totalPages) => ({ ok: true, json: async () => ({ content, totalPages }) });
  const product = (id, name) => ({ id, name, price: 10 });

  test("refreshes before paging and keeps existing products when the next page fails", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn()
      .mockResolvedValueOnce(page([product(1, "My product")], 2))
      .mockResolvedValueOnce({ ok: false });

    render(
      <MemoryRouter>
        <ProductPageForSeller refreshAccessToken={refreshAccessToken} />
      </MemoryRouter>
    );

    expect(await screen.findByText("My product")).toBeInTheDocument();
    fireEvent.click(screen.getByText("2"));

    await waitFor(() => expect(screen.getByText("無法讀取我的商品，請重新登入後再試。")).toBeInTheDocument());
    expect(screen.getByText("My product")).toBeInTheDocument();
    expect(refreshAccessToken).toHaveBeenCalledTimes(2);
  });

  test("shows a login-expired message when refresh fails", async () => {
    const refreshAccessToken = jest.fn(() => Promise.reject(new Error("Unable to refresh access token")));

    render(
      <MemoryRouter>
        <ProductPageForSeller refreshAccessToken={refreshAccessToken} />
      </MemoryRouter>
    );

    expect(await screen.findByText("登入已失效，請重新登入後再試。")).toBeInTheDocument();
  });

  test("keeps the current page when refresh fails during pagination", async () => {
    const refreshAccessToken = jest.fn()
      .mockImplementationOnce((callback) => callback())
      .mockRejectedValueOnce(new Error("Unable to refresh access token"));
    global.fetch = jest.fn().mockResolvedValueOnce(page([product(1, "My product")], 2));

    render(<MemoryRouter><ProductPageForSeller refreshAccessToken={refreshAccessToken} /></MemoryRouter>);
    expect(await screen.findByText("My product")).toBeInTheDocument();
    fireEvent.click(screen.getByText("2"));

    expect(await screen.findByText("登入已失效，請重新登入後再試。")).toBeInTheDocument();
    expect(screen.getByText("My product")).toBeInTheDocument();
  });

  test("shows a login-expired message when refresh fails before deletion", async () => {
    const refreshAccessToken = jest.fn()
      .mockImplementationOnce((callback) => callback())
      .mockRejectedValueOnce(new Error("Unable to refresh access token"));
    global.fetch = jest.fn().mockResolvedValueOnce(page([product(1, "My product")], 1));

    render(
      <MemoryRouter>
        <ProductPageForSeller refreshAccessToken={refreshAccessToken} />
      </MemoryRouter>
    );

    expect(await screen.findByText("My product")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "刪除商品 My product" }));

    expect(await screen.findByText("登入已失效，請重新登入後再試。")).toBeInTheDocument();
  });

  test("keeps the list and shows an error when DELETE fails", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn()
      .mockResolvedValueOnce(page([product(1, "My product")], 1))
      .mockResolvedValueOnce({ ok: false });

    render(<MemoryRouter><ProductPageForSeller refreshAccessToken={refreshAccessToken} /></MemoryRouter>);
    expect(await screen.findByText("My product")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "刪除商品 My product" }));

    expect(await screen.findByText("無法刪除商品，請稍後再試。")).toBeInTheDocument();
    expect(screen.getByText("My product")).toBeInTheDocument();
  });

  test("keeps the list and shows an error when DELETE has a network failure", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn()
      .mockResolvedValueOnce(page([product(1, "My product")], 1))
      .mockRejectedValueOnce(new Error("network down"));

    render(<MemoryRouter><ProductPageForSeller refreshAccessToken={refreshAccessToken} /></MemoryRouter>);
    expect(await screen.findByText("My product")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "刪除商品 My product" }));

    expect(await screen.findByText("無法刪除商品，請稍後再試。")).toBeInTheDocument();
    expect(screen.getByText("My product")).toBeInTheDocument();
  });

  test("falls back to the last valid page after deleting the final item", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn()
      .mockResolvedValueOnce(page([product(1, "First")], 2))
      .mockResolvedValueOnce(page([product(2, "Last")], 2))
      .mockResolvedValueOnce({ ok: true })
      .mockResolvedValueOnce(page([], 1))
      .mockResolvedValueOnce(page([product(1, "First")], 1));

    render(<MemoryRouter><ProductPageForSeller refreshAccessToken={refreshAccessToken} /></MemoryRouter>);
    expect(await screen.findByText("First")).toBeInTheDocument();
    fireEvent.click(screen.getByText("2"));
    expect(await screen.findByText("Last")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "刪除商品 Last" }));

    expect(await screen.findByText("First")).toBeInTheDocument();
    expect(screen.queryByText("Last")).not.toBeInTheDocument();
  });

  test("disables next for an empty catalogue", async () => {
    const refreshAccessToken = jest.fn((callback) => callback());
    global.fetch = jest.fn().mockResolvedValueOnce(page([], 0));

    render(<MemoryRouter><ProductPageForSeller refreshAccessToken={refreshAccessToken} /></MemoryRouter>);

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    expect(screen.getByLabelText("Next").closest("li")).toHaveClass("disabled");
  });
});
