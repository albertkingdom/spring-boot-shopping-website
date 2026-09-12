const backofficeModules = [
  {
    key: "admin-overview",
    label: "概覽",
    path: "/admin",
    workspace: "admin",
    group: "工作台",
    requiredRoles: ["ROLE_ADMIN"],
    end: true,
  },
  {
    key: "admin-products",
    label: "全站商品",
    path: "/admin/products",
    workspace: "admin",
    group: "營運",
    requiredRoles: ["ROLE_ADMIN"],
  },
  {
    key: "admin-orders",
    label: "全站訂單",
    path: "/admin/orders",
    workspace: "admin",
    group: "營運",
    requiredRoles: ["ROLE_ADMIN"],
  },
  {
    key: "admin-sellers",
    label: "商家權限",
    path: "/admin/sellers",
    workspace: "admin",
    group: "管理",
    requiredRoles: ["ROLE_ADMIN"],
  },
  {
    key: "seller-overview",
    label: "概覽",
    path: "/seller",
    workspace: "seller",
    group: "工作台",
    requiredRoles: ["ROLE_SELLER"],
    end: true,
  },
  {
    key: "seller-products",
    label: "我的商品",
    path: "/seller/product_list_page_seller",
    workspace: "seller",
    group: "營運",
    requiredRoles: ["ROLE_SELLER"],
  },
  {
    key: "seller-orders",
    label: "我的訂單",
    path: "/seller/order_list_page_seller",
    workspace: "seller",
    group: "營運",
    requiredRoles: ["ROLE_SELLER"],
  },
];

const groupOrder = ["工作台", "營運", "管理"];

export function getBackofficeNavigation(workspace, userRole = []) {
  const visibleModules = backofficeModules.filter(
    (module) =>
      module.workspace === workspace &&
      module.requiredRoles.some((role) => userRole.includes(role))
  );

  return groupOrder
    .map((group) => ({
      label: group,
      items: visibleModules.filter((module) => module.group === group),
    }))
    .filter((group) => group.items.length > 0);
}

export function findBackofficeModule(pathname, workspace, userRole = []) {
  const modules = getBackofficeNavigation(workspace, userRole)
    .flatMap((group) => group.items)
    .sort((first, second) => second.path.length - first.path.length);

  return modules.find((module) =>
    module.end
      ? pathname === module.path
      : pathname === module.path || pathname.startsWith(`${module.path}/`)
  );
}

export default backofficeModules;
