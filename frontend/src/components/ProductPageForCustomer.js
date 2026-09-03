import { useEffect, useState } from "react";
import { Container, Row, Spinner } from "react-bootstrap";
import ProductListItem from "./ProductListItem";
import Pagination from "./subComponents/Pagination";

function ProductPageForCustomer() {
  const [productList, setProductList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalPage, setTotalPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(0); // page start from 0

  useEffect(() => {
    fetchProductsByPage(0)
  }, []);
  function fetchProductsByPage(page = 0) {
    fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products?page=${page}`)
      .then((response) => response.json())
      .then((data) => {
        setProductList(data.content);
        setIsLoading(false);
        setTotalPage(data.totalPages)
        setCurrentPage(page)
      });
  }
  if (isLoading) {
    return (
      <div className="p-5 d-flex justify-content-center">
        <Spinner animation="border" />
      </div>
    );
  }
  return (
    <div>
      <Container>
        <Row>
          {productList.map((product) => (
            <ProductListItem key={product.id} product={product} />
          ))}
        </Row>
        <Pagination
          totalPage={totalPage}
          currentPage={currentPage}
          fetch={fetchProductsByPage} />
      </Container>

    </div>
  );
}

export default ProductPageForCustomer;
