import { NavLink, useLocation } from "react-router-dom";
import styles from "../style/BackofficeShell.module.css";
import {
  findBackofficeModule,
  getBackofficeNavigation,
} from "./backofficeModules";

const workspaceConfig = {
  admin: {
    label: "平台管理",
    scope: "你正在查看全站資料。",
  },
  seller: {
    label: "商家中心",
    scope: "只顯示本店商品與訂單。",
  },
};

export default function BackofficeShell({ workspace, userRole = [], children }) {
  const location = useLocation();
  const config = workspaceConfig[workspace];
  const navigation = getBackofficeNavigation(workspace, userRole);
  const activeModule = findBackofficeModule(
    location.pathname,
    workspace,
    userRole
  );

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar} aria-label={`${config.label}導覽`}>
        <div className={styles.context}>
          <span className={styles.contextLabel}>目前工作空間</span>
          <h2>{config.label}</h2>
          <p className={styles.scope} role="note">
            {config.scope}
          </p>
        </div>

        <nav className={styles.navigation} aria-label="後台主要導覽">
          {navigation.map((group) => (
            <section className={styles.navigationGroup} key={group.label}>
              <h3>{group.label}</h3>
              <ul>
                {group.items.map((module) => (
                  <li key={module.key}>
                    <NavLink
                      to={module.path}
                      end={module.end}
                      className={({ isActive }) =>
                        isActive ? styles.activeLink : styles.link
                      }
                    >
                      {module.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </nav>
      </aside>

      <main className={styles.main}>
        <div className={styles.breadcrumb} aria-label="breadcrumb">
          後台 / {config.label} / {activeModule?.label || "頁面"}
        </div>
        {children}
      </main>
    </div>
  );
}
