/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.vault.support;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object representing versioned secrets along {@link Version} metadata. A versioned
 * object can hold various states to represent:
 *
 * <ul>
 * <li>Initial (not yet versioned) secrets via {@link Versioned#create(Object)}</li>
 * <li>Versioned secrets via {@link Versioned#create(Object, Version)}</li>
 * <li>Versioned secrets with {@link Metadata} attached
 * {@link Versioned#create(Object, Metadata)}</li>
 * </ul>
 *
 * Versioned secrets follow a lifecycle that spans from creation to destruction:
 *
 * <ol>
 * <li>Creation of an unversioned secret: Secret is not yet persisted.</li>
 * <li>Versioned secret: Secret is persisted.</li>
 * <li>Superseded versioned secret: A newer secret version is stored.</li>
 * <li>Deleted versioned secret: Version was deleted. Can be undeleted.</li>
 * <li>Destroyed versioned secret: Version was destroyed.</li>
 * </ol>
 *
 * @author Mark Paluch
 * @since 2.1
 * @see Version
 * @see Metadata
 */
public class Versioned<T> {

	private final @Nullable T data;

	private final Version version;

	private final @Nullable Metadata metadata;

	private Versioned(T data, Version version) {

		this.version = version;
		this.metadata = null;
		this.data = data;
	}

	private Versioned(@Nullable T data, Version version, Metadata metadata) {

		this.version = version;
		this.metadata = metadata;
		this.data = data;
	}

	/**
	 * Create a {@link Version#unversioned() unversioned} given secret.
	 *
	 * @param secret must not be {@literal null}.
	 * @return the {@link Versioned} object for {@code secret}
	 */
	public static <T> Versioned<T> create(T secret) {

		Assert.notNull(secret, "Versioned data must not be null");

		return new Versioned<>(secret, Version.unversioned());
	}

	/**
	 * Create a versioned secret object given {@code secret} and {@link Version}.
	 * Versioned secret may contain no actual data as they can be in a deleted/destroyed
	 * state.
	 *
	 * @param secret can be {@literal null}.
	 * @param version must not be {@literal null}.
	 * @return the {@link Versioned} object for {@code secret} and {@code Version}.
	 */
	public static <T> Versioned<T> create(@Nullable T secret, Version version) {

		Assert.notNull(version, "Version must not be null");

		return new Versioned<>(secret, version);
	}

	/**
	 * Create a versioned secret object given {@code secret} and {@link Metadata}.
	 * Versioned secret may contain no actual data as they can be in a deleted/destroyed
	 * state.
	 *
	 * @param secret can be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @return the {@link Versioned} object for {@code secret} and {@link Metadata}.
	 */
	public static <T> Versioned<T> create(@Nullable T secret, Metadata metadata) {

		Assert.notNull(metadata, "Metadata must not be null");

		return new Versioned<>(secret, metadata.getVersion(), metadata);
	}

	/**
	 * @return the {@link Version} associated with this {@link Versioned} object.
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * @return {@literal true} if this versioned object has {@link Metadata} associated,
	 * otherwise {@code false}
	 */
	public boolean hasMetadata() {
		return metadata != null;
	}

	@Nullable
	public Metadata getMetadata() {
		return metadata;
	}

	/**
	 * @return {@literal true} if this versioned object has data associated, or
	 * {@code false}, of the version is deleted or destroyed.
	 */
	public boolean hasData() {
		return data != null;
	}

	/**
	 * @return the actual data for this versioned object. Can be {@literal null} if the
	 * version is deleted or destroyed.
	 */
	@Nullable
	public T getData() {
		return data;
	}

	/**
	 * Returns the required data for this versioned object. Throws
	 * {@link IllegalStateException} if no data is associated.
	 *
	 * @return the non-null value held by this for this versioned object.
	 * @throws IllegalStateException if no data is present.
	 */
	public T getRequiredData() {

		T data = this.data;

		if (data == null) {
			throw new IllegalStateException("Required data is not present");
		}

		return data;
	}

	/**
	 * Convert the data element of this versioned object to an {@link Optional}.
	 *
	 * @return {@link Optional#of(Object) Optional} holding the actual value of this
	 * versioned object if {@link #hasData() data is present}, {@link Optional#empty()} if
	 * no data is associated.
	 */
	public Optional<T> toOptional() {
		return Optional.ofNullable(data);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Versioned))
			return false;
		Versioned<?> versioned = (Versioned<?>) o;
		return Objects.equals(data, versioned.data)
				&& Objects.equals(version, versioned.version)
				&& Objects.equals(metadata, versioned.metadata);
	}

	@Override
	public int hashCode() {

		return Objects.hash(data, version, metadata);
	}

	/**
	 * Value object representing version metadata such as creation/deletion time.
	 */
	public static class Metadata {

		private final Instant createdAt;

		private final @Nullable Instant deletedAt;

		private final boolean destroyed;

		private final Version version;

		private Metadata(Instant createdAt, @Nullable Instant deletedAt,
				boolean destroyed, Version version) {
			this.createdAt = createdAt;
			this.deletedAt = deletedAt;
			this.destroyed = destroyed;
			this.version = version;
		}

		/**
		 * Creates a new {@link MetadataBuilder} to build {@link Metadata} objects.
		 *
		 * @return a new {@link MetadataBuilder} to build {@link Metadata} objects.
		 */
		public static MetadataBuilder builder() {
			return new MetadataBuilder();
		}

		/**
		 * @return {@link Instant} at which the version was created.
		 */
		public Instant getCreatedAt() {
			return createdAt;
		}

		/**
		 * @return {@literal true} if the version was deleted.
		 */
		public boolean isDeleted() {
			return deletedAt != null;
		}

		/**
		 * @return {@link Instant} at which the version was deleted. Can be
		 * {@literal null} if the version is not deleted.
		 */
		@Nullable
		public Instant getDeletedAt() {
			return deletedAt;
		}

		/**
		 * @return the version number.
		 */
		public Version getVersion() {
			return version;
		}

		/**
		 * @return {@literal true} if the version was destroyed.
		 */
		public boolean isDestroyed() {
			return destroyed;
		}

		@Override
		public String toString() {

			return getClass().getSimpleName() + " [createdAt=" + createdAt
					+ ", deletedAt=" + deletedAt + ", destroyed=" + destroyed
					+ ", version=" + version + ']';
		}

		/**
		 * Builder for {@link Metadata} objects.
		 */
		public static class MetadataBuilder {

			private @Nullable Instant createdAt;

			private @Nullable Instant deletedAt;

			private boolean destroyed;

			private @Nullable Version version;

			private MetadataBuilder() {
			}

			/**
			 * Configure a created at {@link Instant}.
			 *
			 * @param createdAt timestamp at which the version was created, must not be
			 * {@literal null}.
			 * @return {@code this} {@link MetadataBuilder}.
			 */
			public MetadataBuilder createdAt(Instant createdAt) {

				Assert.notNull(createdAt, "Created at must not be null");

				this.createdAt = createdAt;
				return this;
			}

			/**
			 * Configure a deleted at {@link Instant}.
			 *
			 * @param deletedAt timestamp at which the version was deleted, must not be
			 * {@literal null}.
			 * @return {@code this} {@link MetadataBuilder}.
			 */
			public MetadataBuilder deletedAt(Instant deletedAt) {

				Assert.notNull(deletedAt, "Deleted at must not be null");

				this.deletedAt = deletedAt;
				return this;
			}

			/**
			 * Configure the version was destroyed.
			 *
			 * @return {@code this} {@link MetadataBuilder}.
			 */
			public MetadataBuilder destroyed() {
				return destroyed(true);
			}

			/**
			 * Configure the version was destroyed.
			 *
			 * @param destroyed
			 * @return {@code this} {@link MetadataBuilder}.
			 */
			public MetadataBuilder destroyed(boolean destroyed) {
				this.destroyed = destroyed;
				return this;
			}

			/**
			 * Configure the {@link Version}.
			 *
			 * @param version must not be {@literal null}.
			 * @return {@code this} {@link MetadataBuilder}.
			 */
			public MetadataBuilder version(Version version) {

				Assert.notNull(version, "Version must not be null!");

				this.version = version;
				return this;
			}

			/**
			 * Build the {@link Metadata} object. Requires {@link #createdAt(Instant)} and
			 * {@link #version(Version)} to be set.
			 *
			 * @return the {@link Metadata} object.
			 */
			public Metadata build() {

				Assert.notNull(createdAt, "CreatedAt must not be null");
				Assert.notNull(version, "Version must not be null");

				return new Metadata(createdAt, deletedAt, destroyed, version);
			}
		}
	}

	/**
	 * Value object representing a Vault version.
	 * <p/>
	 * Versions greater zero point to a specific secret version whereas version number
	 * zero points to a placeholder whose meaning is tied to a specific operation. Version
	 * number zero can mean first created version, latest version.
	 *
	 * @author Mark Paluch
	 */
	public static class Version {

		static final Version UNVERSIONED = new Version(0);

		private final int version;

		private Version(int version) {
			this.version = version;
		}

		/**
		 * @return the unversioned {@link Version} as placeholder for specific operations
		 * that require version number zero.
		 */
		public static Version unversioned() {
			return UNVERSIONED;
		}

		/**
		 * Create a {@link Version} given a {@code versionNumber}.
		 *
		 * @param versionNumber the version number.
		 * @return the {@link Version} for {@code versionNumber}.
		 */
		public static Version from(int versionNumber) {

			if (versionNumber > 0) {
				return new Version(versionNumber);
			}

			return UNVERSIONED;
		}

		/**
		 * @return {@literal true} if this {@link Version} points to a valid version
		 * number, {@literal false} otherwise.
		 * <p/>
		 * Version numbers that are equal zero are placeholders to denote unversioned or
		 * latest versions in the context of particular versioning operations.
		 */
		public boolean isVersioned() {
			return version > 0;
		}

		/**
		 * @return the version number.
		 */
		public int getVersion() {
			return version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Version))
				return false;
			Version version1 = (Version) o;
			return version == version1.version;
		}

		@Override
		public int hashCode() {
			return Objects.hash(version);
		}

		@Override
		public String toString() {
			return String.format("Version[%d]", version);
		}
	}
}
