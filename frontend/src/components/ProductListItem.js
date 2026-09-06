import { Button, Card, Col } from "react-bootstrap";
import { useNavigate, useParams } from "react-router-dom";
import styles from "../style/Product.module.css";

export default function ProductListItem({ product }) {
  let navigate = useNavigate();
  const imgSrc = product.imgUrl || "https://via.placeholder.com/728";
  return (
    <Col sm={3}>
      <Card
        className={`${styles.product_card} my-2`}
        onClick={() => {
          navigate(`/product_detail/${product.id}`);
        }}
      >
        <Card.Img variant="top" src={imgSrc} />
        <Card.Body>
          <Card.Title>{product.name}</Card.Title>
          <Card.Text>${product.price}</Card.Text>
          {/* <Button variant="primary">Go somewhere</Button> */}
        </Card.Body>
      </Card>
    </Col>
  );
}
