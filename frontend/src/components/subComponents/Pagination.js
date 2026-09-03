import { Link } from "react-router-dom";

export default function Pagination({ totalPage, currentPage, fetch }) {
    function onClickNextPage(e) {
        e.preventDefault();
        if (currentPage + 1 === totalPage) {
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
            <ul class="pagination justify-content-center">
                <li class={`page-item ${currentPage === 0 ? "disabled" : ""}`}>
                    <Link class="page-link" to="#" aria-label="Previous" onClick={onClickPreviousPage}>
                        <span aria-hidden="true">&laquo;</span>
                    </Link>
                </li>
                {Array.from({ length: totalPage }, (_, i) => i + 1).map(page =>
                    <li key={page} className={`page-item ${currentPage === page - 1 ? `active` : ""}`}>
                        <Link class="page-link" to="#" onClick={e => onClickPage(e, page)}>{page}</Link>
                    </li>

                )}

                <li class={`page-item ${currentPage + 1 === totalPage ? "disabled" : ""}`}>
                    <Link class="page-link" to="#" aria-label="Next" onClick={onClickNextPage}>
                        <span aria-hidden="true">&raquo;</span>
                    </Link>
                </li>
            </ul>
        </nav>
    )
}