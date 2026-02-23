#
# © 2026-present https://github.com/cengiz-pz
#

require 'xcodeproj'

def print_usage
	puts "Usage: ruby spm_manager.rb -a|-d <path_to_xcodeproj> <dependency1> [dependency2 ...]"
	puts ""
	puts "Options:"
	puts "  -a    Add the specified SPM dependencies to the Xcode project"
	puts "  -d    Remove the specified SPM dependencies from the Xcode project"
	puts ""
	puts "Example:"
	puts "  ruby spm_manager.rb -a MyProject.xcodeproj \"https://github.com/URL1|Version1|ProductName1\" \"https://github.com/URL2|Version2|ProductName2\""
	puts "  ruby spm_manager.rb -d MyProject.xcodeproj \"https://github.com/URL1|Version1|ProductName1\""
end

# Argument Validation
if ARGV.length < 3
	print_usage
	exit 1
end

option = ARGV[0]

unless ['-a', '-d'].include?(option)
	puts "Error: Unknown option '#{option}'. Must be -a (add) or -d (remove)."
	puts ""
	print_usage
	exit 1
end

project_path = ARGV[1]
deps = ARGV[2..-1]

unless File.exist?(project_path)
	puts "Error: Xcode project not found at #{project_path}"
	exit 1
end

# Xcode Project Manipulation
begin
	project = Xcodeproj::Project.open(project_path)
	# Target selection logic (defaults to the first target)
	target = project.targets.first

	if target.nil?
		puts "Error: No targets found in the Xcode project."
		exit 1
	end

	if option == '-a'
		# Clear existing SPM packages to avoid duplicates on rebuilds
		project.root_object.package_references.clear
		target.package_product_dependencies.clear

		# Dynamically inject SPM dependencies
		deps.each do |dep|
			next if dep.empty?
			# Expected format: "https://github.com/URL|Version|ProductName"
			parts = dep.split('|').map(&:strip)

			if parts.size == 3
				url, version, product_name = parts

				# Create the remote SPM package reference
				pkg = project.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)
				pkg.repositoryURL = url
				pkg.requirement = {
					'kind' => 'upToNextMajorVersion',
					'minimumVersion' => version
				}
				project.root_object.package_references << pkg

				# Create the product dependency and link it to the target
				ref = project.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)
				ref.product_name = product_name
				ref.package = pkg
				target.package_product_dependencies << ref
			else
				puts "Warning: Skipping invalid SPM dependency format: #{dep}. Expected 'URL|Version|ProductName'\n\n"
			end
		end

		puts "Successfully updated SPM dependencies in #{File.basename(project_path)}\n\n"

	elsif option == '-d'
		# Parse the dependency specs to determine which URLs and product names to remove
		urls_to_remove = []
		product_names_to_remove = []

		deps.each do |dep|
			next if dep.empty?
			parts = dep.split('|').map(&:strip)

			if parts.size == 3
				url, _version, product_name = parts
				urls_to_remove << url
				product_names_to_remove << product_name
			else
				puts "Warning: Skipping invalid SPM dependency format: #{dep}. Expected 'URL|Version|ProductName'\n\n"
			end
		end

		# Collect objects to remove, then delete them from both the array and the project
		# object graph so their definitions are fully purged from project.pbxproj
		deps_to_remove = target.package_product_dependencies.select do |dep|
			product_names_to_remove.include?(dep.product_name)
		end
		deps_to_remove.each do |dep|
			target.package_product_dependencies.delete(dep)
			dep.remove_from_project
		end

		pkgs_to_remove = project.root_object.package_references.select do |pkg|
			urls_to_remove.include?(pkg.repositoryURL)
		end
		pkgs_to_remove.each do |pkg|
			project.root_object.package_references.delete(pkg)
			pkg.remove_from_project
		end

		puts "Successfully removed SPM dependencies from #{File.basename(project_path)}\n\n"
	end

	project.save

rescue => e
	puts "An error occurred: #{e.message}\n\n"
	exit 1
end
