import { useParams } from "react-router-dom";
import { useEffect, useState } from "react"
import { Container, Row, Col, Button } from "react-bootstrap"

function ProductDetailPageForCustomer({setCartCount}) {
    let { id } = useParams()
    const [productName, setProductName] = useState("")
    const [productPrice, setProductPrice] = useState(0)
    const [product, setProduct] = useState({})
    useEffect(() => {
        fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products/${id}`)
            .then(response => response.json())
            .then(data => {
                // setProductName(data.name)
                // setProductPrice(data.price)
                setProduct(data)
            })
    }, [id])
    
    function handleAddToCart(productId, productCount) {
        let existing = localStorage.getItem("shopping_cart") //[ {id:1, count:3},{...}]
        let existingArray = JSON.parse(existing)
        console.log("existing array length", existingArray)
        if (existing != null && existingArray.length > 0) {
            let existingArray = JSON.parse(existing)
            // let newArray = existingArray.map(item => item.id === productId ? { id: item.id, count: item.count += productCount } : item)
 
            if (existingArray.filter(item=>item.id === productId).length > 0){
                // product id is in array
                let newArray = existingArray.map(item => item.id === productId ? { id: item.id, count: item.count += productCount } : item)
                localStorage.setItem("shopping_cart", JSON.stringify(newArray))
            } else {
                existingArray.push({ id: productId, count: productCount })
                localStorage.setItem("shopping_cart", JSON.stringify(existingArray))
            }
            
        } else {
            let newArray = [{ id: productId, count: productCount }]
            localStorage.setItem("shopping_cart", JSON.stringify(newArray))
        }
        calculateCartCount()
    }
    function calculateCartCount(){
        let existing = localStorage.getItem("shopping_cart")
        let existingArray = JSON.parse(existing)
        let total = 0
        existingArray.map(item => total += item.count)
        setCartCount(total)
    }
    const imgSrc = product.imgUrl || "https://via.placeholder.com/728";
    return (

        <Container className="py-3">
            <Row>
                <Col sm={6}>
                    <img src={imgSrc} alt="product" className="img-fluid img-thumbnail"/>
                </Col>
                <Col sm={6}>
                    <h3>{product.name}</h3>
                    <h4>${product.price}</h4>
                    <Button variant="outline-danger" onClick={() => handleAddToCart(id, 1)}>加入購物車</Button>
                </Col>
            </Row>

        </Container>
    )
}

export default ProductDetailPageForCustomer