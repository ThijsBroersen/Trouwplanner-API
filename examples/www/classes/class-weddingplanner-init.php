<?php
	class WeddingplannerInit {

		public function __construct() {

			$wedding_api_url = 'http' . (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 's' : '') . '://' . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'];

			// define global path
			if ( ! defined('WEDDING_API_DIR'))  define('WEDDING_API_DIR', realpath(__DIR__ . '/..'));
			if ( ! defined('WEDDING_API_URL'))  define('WEDDING_API_URL', rtrim( dirname($wedding_api_url), '/' ) );
			
			// include the helper functions
			require_once WEDDING_API_DIR . '/lib/helpers.php';

			// include all the classes
			require_once WEDDING_API_DIR . '/classes/class-weddingplanner-view.php';

		}

		public function init_view() {

			$variables = array(
				'locations_list' 		=> weddingplanner_location_list('ID', 'Name'),
				'functionaries_list' 	=> weddingplanner_functionary_list('ID', 'Name'),
			);

			WeddingPlannerView::get_index( $variables );

		}

	}