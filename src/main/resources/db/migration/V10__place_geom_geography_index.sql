CREATE INDEX idx_place_geom_geography_published ON public.place USING gist ((geom::geography)) WHERE (is_published = true);
