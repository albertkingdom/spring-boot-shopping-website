import { Link } from "react-router-dom";

export default function Pagination({ totalPage, currentPage, fetch }) {
    function onClickNextPage(e) {
        e.preventDefault();
        if (currentPage >= totalPage - 1) {
            return
        }
        fetch(currentPage + 1)
    }
    function onClickPreviousPage(e) {
        e.preventDefault();
        if (currentPage === 0) {
            return
        }
        fetch(currentPage - 1)
    }
    function onClickPage(e, pageVisable) {
        e.preventDefault();
        fetch(pageVisable - 1)
    }
    return (
        <nav aria-label="Page navigations">
            <ul className="pagination justify-content-center">
                <li className={`page-item ${currentPage === 0 ? "disabled" : ""}`}>
                    <Link className="page-link" to="#" aria-label="Previous" onClick={onClickPreviousPage}>
                        <span aria-hidden="true">&laquo;</span>
                    </Link>
                </li>
                {Array.from({ length: totalPage }, (_, i) => i + 1).map(page =>
                    <li key={page} className={`page-item ${currentPage === page - 1 ? `active` : ""}`}>
                        <Link className="page-link" to="#" onClick={e => onClickPage(e, page)}>{page}</Link>
                    </li>

                )}

                <li className={`page-item ${currentPage >= totalPage - 1 ? "disabled" : ""}`}>
                    <Link className="page-link" to="#" aria-label="Next" onClick={onClickNextPage}>
                        <span aria-hidden="true">&raquo;</span>
                    </Link>
                </li>
            </ul>
        </nav>
    )
}
