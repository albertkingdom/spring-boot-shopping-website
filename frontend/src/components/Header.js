import { Navbar, Container, Badge, Nav } from "react-bootstrap";
import { Link } from "react-router-dom";
import { IoCartOutline, IoPersonCircleOutline } from "react-icons/io5";
import styles from "../style/Header.module.css"

export default function Header({ userInfo, cartCount }) {
  return (
    <Navbar bg="dark" expand="lg" variant="dark">
      <Container className="d-flex align-items-center">
        <Navbar.Brand>Ecommerce</Navbar.Brand>
        <Navbar.Toggle aria-controls="responsive-navbar-nav" />
        <Navbar.Collapse id="responsive-navbar-nav">
          <Nav className="me-auto">
            <Link
              to="/seller"
              className="text-decoration-none text-light mx-2 nav-link"
            >
              後台
            </Link>
            <Link
              to="/product_list"
              className="text-decoration-none text-light mx-2 nav-link"
            >
              商品頁
            </Link>
          </Nav>
          <Nav>
            <Link to="/cart" className="mx-2 nav-link">
              <IoCartOutline size={24} className="text-light" />
              <Badge bg="secondary" className="d-inline-block p-2">
                {cartCount}
              </Badge>
            </Link>

            <Link to="/login" className="mx-2 nav-link">
              {userInfo && <span className="my-0">{userInfo}</span>}
              <IoPersonCircleOutline size={24} className="text-light" />
            </Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
