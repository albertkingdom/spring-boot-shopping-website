import { BrowserRouter as Router, Navigate, Routes, Route } from "react-router-dom";

import Header from "./components/Header";
import ProductPageForSeller from "./components/ProductPageForSeller";
import CreateProduct from "./components/CreateProduct";
import EditProduct from "./components/EditProduct";
import SellerOrderDetail from "./components/SellerOrderDetail";
import SellerRoleManagement from "./components/SellerRoleManagement";
import Admin from "./components/Admin";
import AdminHome from "./components/AdminHome";
import AdminProductPage from "./components/AdminProductPage";
import AdminOrderList from "./components/AdminOrderList";
import AdminOrderDetail from "./components/AdminOrderDetail";
import ProductPageForCustomer from "./components/ProductPageForCustomer";
import OrderListPageForSeller from "./components/OrderListPageForSeller";
import ProductDetailPageForCustomer from "./components/ProductDetailPageForCustomer";
import Cart from "./components/Cart";
import Login from "./components/Login";
import Seller from "./components/Seller";
import SellerHome from "./components/SellerHome";
import { useState } from "react";
import RouteNeedAdmin from "./components/RouteNeedAdmin";
import RouteNeedSeller from "./components/RouteNeedSeller";
import jwt_decode from "jwt-decode";
import { ThemeProvider } from "./components/ThemeContext";

function App() {
  const [userInfo, setUserInfo] = useState(() => sessionStorage.getItem("shopping-website-user"));
  const [userRole, setUserRole] = useState(() => {
    const accessToken = sessionStorage.getItem("access_token");
    if (!accessToken) {
      return [];
    }
    try {
      return jwt_decode(accessToken).roles || [];
    } catch (error) {
      return [];
    }
  });
  const [cartCount, setCartCount] = useState(0);
  
  function configuretUserInfo(value) {
    setUserInfo(value);
  }
  function configureUserRole(role) {
    setUserRole(role)
  }
  function setCartCountCallback(value) {
    setCartCount(value);
  }
  return (
    <ThemeProvider>
      <Router>
        <Header userInfo={userInfo} userRole={userRole} cartCount={cartCount} />

        <Routes>
        <Route path="/" element={<Navigate to="/product_list" replace />} />
        <Route
          path="/seller"
          element={
            <RouteNeedSeller redirectTo="/login" userName={userInfo} userRole={userRole}>
              <Seller userRole={userRole} />
            </RouteNeedSeller>
          }
        >
          <Route index element={<SellerHome />} />
          <Route
            path="product_list_page_seller"
            element={<ProductPageForSeller />}
          />
          <Route
            path="order_list_page_seller"
            element={<OrderListPageForSeller />}
          />
          <Route path="createProduct" element={<CreateProduct />} />
          <Route path="editProduct/:id" element={<EditProduct />} />
          <Route path="orders/:id" element={<SellerOrderDetail />} />
        </Route>

        <Route
          path="/admin"
          element={
            <RouteNeedAdmin redirectTo="/login" userName={userInfo} userRole={userRole}>
              <Admin userRole={userRole} />
            </RouteNeedAdmin>
          }
        >
          <Route index element={<AdminHome />} />
          <Route path="sellers" element={<SellerRoleManagement />} />
          <Route path="products" element={<AdminProductPage />} />
          <Route path="products/create" element={<CreateProduct returnTo="/admin/products" createEndpoint="/api/admin/products" />} />
          <Route path="products/:id" element={<EditProduct returnTo="/admin/products" />} />
          <Route path="orders" element={<AdminOrderList />} />
          <Route path="orders/:id" element={<AdminOrderDetail />} />
          <Route path="*" element={<Navigate to="/admin" replace />} />
        </Route>

        <Route path="/product_list" element={<ProductPageForCustomer />} />
        <Route
          path="/product_detail/:id"
          element={<ProductDetailPageForCustomer setCartCount={setCartCountCallback} />}
        />
        <Route path="/cart" element={<Cart setCartCount={setCartCountCallback} />} />
        
        <Route
          path="/login"
          element={<Login userName={userInfo} setUser={configuretUserInfo} setRole={configureUserRole}/>}
        />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;
