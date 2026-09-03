import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Header from "./components/Header";
import ProductPageForSeller from "./components/ProductPageForSeller";
import CreateProduct from "./components/CreateProduct";
import EditProduct from "./components/EditProduct";
import EditOrder from "./components/EditOrder";
import ProductPageForCustomer from "./components/ProductPageForCustomer";
import OrderListPageForSeller from "./components/OrderListPageForSeller";
import ProductDetailPageForCustomer from "./components/ProductDetailPageForCustomer";
import Cart from "./components/Cart";
import Login from "./components/Login";
import Seller from "./components/Seller";
import SellerHome from "./components/SellerHome";
import { useState } from "react";
import RouteNeedLogin from "./components/RouteNeedLogin";
import RouteNeedAdmin from "./components/RouteNeedAdmin";

function App() {
  const [userInfo, setUserInfo] = useState(null);
  const [userRole, setUserRole] = useState([]);
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
    <Router>
      <Header userInfo={userInfo} cartCount={cartCount} />

      <Routes>
        <Route
          path="/seller"
          element={
            <RouteNeedAdmin redirectTo="/login" userName={userInfo} userRole={userRole}>
              <Seller />
            </RouteNeedAdmin>
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
          <Route path="editOrder/:id" element={<EditOrder />} />
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
  );
}

export default App;
