import { Navbar, Container, Badge, Nav, Dropdown } from "react-bootstrap";
import { Link } from "react-router-dom";
import {
  IoCartOutline,
  IoDesktopOutline,
  IoMoonOutline,
  IoPersonCircleOutline,
  IoSunnyOutline,
} from "react-icons/io5";
import { useTheme } from "./ThemeContext";
import styles from "../style/Header.module.css";

const themeOptions = {
  system: { label: "跟隨系統", shortLabel: "系統", Icon: IoDesktopOutline },
  light: { label: "淺色", shortLabel: "淺色", Icon: IoSunnyOutline },
  dark: { label: "深色", shortLabel: "深色", Icon: IoMoonOutline },
};

export default function Header({ userInfo, userRole, cartCount }) {
  const { theme, setTheme } = useTheme();
  const selectedTheme = themeOptions[theme] || themeOptions.system;
  const ThemeIcon = selectedTheme.Icon;

  return (
    <Navbar expand="lg" className={styles.topHeader}>
      <Container className="d-flex align-items-center">
        <Navbar.Brand className={styles.brand}>Ecommerce</Navbar.Brand>
        <Navbar.Toggle
          aria-controls="responsive-navbar-nav"
          aria-label="切換導覽"
          className={styles.toggle}
        />
        <Navbar.Collapse id="responsive-navbar-nav">
          <Nav className="me-auto">
            {userRole.includes("ROLE_SELLER") && (
              <Link to="/seller" className={styles.navLink}>
                商家中心
              </Link>
            )}
            {userRole.includes("ROLE_ADMIN") && (
              <Link to="/admin" className={styles.navLink}>
                平台管理
              </Link>
            )}
            <Link
              to="/product_list"
              className={styles.navLink}
            >
              商品頁
            </Link>
          </Nav>
          <Nav className={styles.actions}>
            <Dropdown className={styles.themeMenu} align="end">
              <Dropdown.Toggle
                as="button"
                id="theme-menu"
                className={styles.themeButton}
                aria-label={`顏色模式：${selectedTheme.label}`}
                title={`顏色模式：${selectedTheme.label}`}
              >
                <ThemeIcon size={19} aria-hidden="true" />
                <span className={styles.themeButtonLabel}>{selectedTheme.shortLabel}</span>
              </Dropdown.Toggle>
              <Dropdown.Menu className={styles.themeMenuList} aria-label="顏色模式選項">
                <Dropdown.Header>顏色模式</Dropdown.Header>
                {Object.entries(themeOptions).map(([value, option]) => {
                  const OptionIcon = option.Icon;
                  return (
                    <Dropdown.Item
                      key={value}
                      active={theme === value}
                      onClick={() => setTheme(value)}
                    >
                      <OptionIcon className={styles.themeOptionIcon} aria-hidden="true" />
                      <span>{option.label}</span>
                    </Dropdown.Item>
                  );
                })}
              </Dropdown.Menu>
            </Dropdown>

            <Link to="/cart" className={styles.navLink} aria-label="購物車">
              <IoCartOutline size={24} className={styles.icon} aria-hidden="true" />
              <Badge className={styles.cartBadge}>
                {cartCount}
              </Badge>
            </Link>

            <Link to="/login" className={styles.navLink} aria-label="登入或帳號">
              {userInfo && <span className={styles.accountName}>{userInfo}</span>}
              <IoPersonCircleOutline size={24} className={styles.icon} aria-hidden="true" />
            </Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
