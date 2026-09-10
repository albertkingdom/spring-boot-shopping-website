import { Outlet } from "react-router-dom";
import AdminSideNav from "./AdminSideNav";
import styles from "../style/App.module.css";

export default function Admin() {
  return (
    <div className={`${styles.main_content} d-flex flex-column flex-md-row`}>
      <AdminSideNav />
      <div className="pt-3 col-md-10">
        <Outlet />
      </div>
    </div>
  );
}
