package com.jdamcd.tflarrivals

object Fixtures {

    const val STOPS_CSV_1 =
"""
stop_id,stop_name,stop_lat,stop_lon,location_type,parent_station
G28,Nassau Av,40.724635,-73.951277,1,
G28N,Nassau Av,40.724635,-73.951277,,G28
G28S,Nassau Av,40.724635,-73.951277,,G28
F27,Church Av,40.644041,-73.979678,1,
F27N,Church Av,40.644041,-73.979678,,F27
F27S,Church Av,40.644041,-73.979678,,F27
G22,Court Sq,40.746554,-73.943832,1,
G22N,Court Sq,40.746554,-73.943832,,G22
G22S,Court Sq,40.746554,-73.943832,,G22
"""

    const val STOPS_CSV_2 =
"""
stop_lat,wheelchair_boarding,stop_code,stop_lon,stop_timezone,stop_url,parent_station,stop_desc,stop_name,location_type,stop_id,zone_id
45.623013,0,1141,-122.625739,,,,,Evergreen Blvd & Farview Dr,0,1141,
45.663343,0,2172,-122.559985,,,,,Gher Rd & Coxley Dr,0,2172,
45.657287,0,4149,-122.666827,,,,,Hazel Dell Ave 4900 Block,0,4149,
45.660011,0,4148,-122.667381,,,,,Hazel Dell Ave 5200 Block,0,4148,
45.67769,0,2173,-122.569274,,,,,NE 76th St & 101st Ave,0,2173,
45.714981,0,4145,-122.651305,,,,,Hwy 99 & 129th St,0,4145,
45.665885,0,4147,-122.668916,,,,,Hazel Dell Ave & 60th St,0,4147,
45.678661,0,3253,-122.635948,,,,,NE 78th St 3300 Block,0,3253,
45.696539,0,3254,-122.654579,,,,,Hwy 99 & NE 104th St,0,3254,
"""
}
