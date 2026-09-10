import { Navigate } from "react-router-dom";

export default function RouteNeedSeller({ children, redirectTo, userRole, userName }) {
  if (!userName) {
    return <Navigate to={redirectTo} />;
  }
  if (userRole.includes("ROLE_SELLER")) {
    return children;
  }
  return <h1>你沒有商家中心的權限。</h1>;
}
