.PHONY: build-image check-manifests install-chart uninstall-chart

VERSION = $(shell ./gradlew getVersion -q)

build-image:
	@echo "Building image with version $(Version)"
	docker build --build-arg VERSION=$(VERSION) -t krank:$(VERSION) .

check-manifests:
	@echo "Checking helm generated k8s manifests"
	helm template krank ./krank-chart --debug

install-chart:
	@echo "Installing krank helm chart"
	helm install krank ./krank-chart

uninstall-chart:
	@echo "Uninstalling krank helm chart"
	helm uninstall krank
