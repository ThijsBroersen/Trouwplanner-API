<?php
	if ( ! defined('WEDDING_API_DIR')) { exit; }
	
	class WeddingPlannerView {

		public static function get_header() {

			// include the header view
			include WEDDING_API_DIR . '/views/view-header.php';

		}

		public static function get_footer() {

			// include the footer view
			include WEDDING_API_DIR . '/views/view-footer.php';

		}

		public static function get_index( $data = array() ) {

			// extract the variables
			extract($data);

			// include the index view
			include WEDDING_API_DIR . '/views/view-index.php';

		}

	}