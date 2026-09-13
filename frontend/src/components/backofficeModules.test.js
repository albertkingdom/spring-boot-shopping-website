import {
  findBackofficeModule,
  getBackofficeNavigation,
} from "./backofficeModules";

test("builds admin navigation from the shared module registry", () => {
  const navigation = getBackofficeNavigation("admin", ["ROLE_ADMIN"]);

  expect(navigation.map((group) => group.label)).toEqual(["工作台", "營運", "管理"]);
  expect(navigation.flatMap((group) => group.items).map((item) => item.label)).toEqual([
    "概覽",
    "全站商品",
    "全站訂單",
    "商家權限",
  ]);
});

test("filters modules by workspace and required role", () => {
  const sellerNavigation = getBackofficeNavigation("seller", ["ROLE_SELLER"]);
  const sellerLabels = sellerNavigation
    .flatMap((group) => group.items)
    .map((item) => item.label);

  expect(sellerLabels).toEqual(["概覽", "我的商品", "我的訂單"]);
  expect(sellerLabels).not.toContain("商家權限");
});

test("matches the most specific module for nested routes", () => {
  expect(
    findBackofficeModule("/admin/products/12", "admin", ["ROLE_ADMIN"])
  ).toMatchObject({ key: "admin-products", label: "全站商品" });
});
