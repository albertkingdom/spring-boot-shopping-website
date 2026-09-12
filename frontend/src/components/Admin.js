import { Outlet } from "react-router-dom";
import BackofficeShell from "./BackofficeShell";

export default function Admin({ userRole = [] }) {
  return (
    <BackofficeShell workspace="admin" userRole={userRole}>
      <Outlet />
    </BackofficeShell>
  );
}
