import { NavLink } from "react-router-dom";
import styles from "../style/App.module.css";

export default function AdminSideNav() {
  return (
    <div className={`col-md-2 ${styles.side_nav}`}>
      <ul>
        <li><NavLink to="/admin/sellers">商家權限</NavLink></li>
        <li><NavLink to="/admin/products">全站商品</NavLink></li>
        <li><NavLink to="/admin/orders">全站訂單</NavLink></li>
      </ul>
    </div>
  );
}
