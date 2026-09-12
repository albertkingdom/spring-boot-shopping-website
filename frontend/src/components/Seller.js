import { Outlet } from "react-router-dom";
import BackofficeShell from "./BackofficeShell";

export default function Seller({ userRole = [] }) {
  return (
    <BackofficeShell workspace="seller" userRole={userRole}>
      <Outlet />
    </BackofficeShell>
  );
}
