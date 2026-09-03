import { NavLink } from "react-router-dom";
import styles from "../style/App.module.css";

export default function SideNav() {
  return (
    <div className={`col-2 ${styles.side_nav}`}>
      <ul>
        <li>
          <NavLink
            to="/seller/product_list_page_seller"
            className={({ isActive }) => (isActive ? "fw-bold" : null)}
          >
            商品管理
          </NavLink>
        </li>
        <li>
          <NavLink
            to="/seller/order_list_page_seller"
            className={({ isActive }) => (isActive ? "fw-bold" : null)}
          >
            訂單管理
          </NavLink>
        </li>
      </ul>
    </div>
  );
}
