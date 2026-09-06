import SideNav from "./SideNav";
import styles from "../style/App.module.css";
import { Outlet } from "react-router-dom";

export default function Seller() {
  return (
    <div className={`${styles.main_content} d-flex`}>
      <SideNav />
      <div className="pt-3 col-10">
        <Outlet />
      </div>
      
      
    </div>
  );
}
