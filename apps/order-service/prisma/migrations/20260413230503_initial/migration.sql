-- CreateEnum
CREATE TYPE "OrderStatus" AS ENUM ('PENDING', 'ACCEPTED', 'IN_PROGRESS', 'DELIVERED', 'REVISION_REQUESTED', 'COMPLETED', 'CANCELLED', 'DISPUTED');

-- CreateEnum
CREATE TYPE "CustomOfferStatus" AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED');

-- CreateTable
CREATE TABLE "orders" (
    "id" BIGSERIAL NOT NULL,
    "client_id" INTEGER NOT NULL,
    "gig_id" INTEGER NOT NULL,
    "order_date" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "status" "OrderStatus" NOT NULL DEFAULT 'PENDING',
    "total_cost" DECIMAL(19,2) NOT NULL,
    "delivery_deadline" TIMESTAMP(3),
    "max_revisions" INTEGER NOT NULL DEFAULT 3,
    "used_revisions" INTEGER NOT NULL DEFAULT 0,
    "completed_at" TIMESTAMP(3),
    "cancelled_at" TIMESTAMP(3),

    CONSTRAINT "orders_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "order_history" (
    "id" BIGSERIAL NOT NULL,
    "order_id" BIGINT NOT NULL,
    "changed_by_user_id" BIGINT NOT NULL,
    "action_type" VARCHAR(255) NOT NULL,
    "old_status" VARCHAR(255) NOT NULL,
    "new_status" VARCHAR(255) NOT NULL,
    "note" VARCHAR(255),
    "changed_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "order_history_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "deliveries" (
    "id" BIGSERIAL NOT NULL,
    "order_id" BIGINT NOT NULL,
    "version_number" INTEGER NOT NULL,
    "message" TEXT,
    "file_url" VARCHAR(500),
    "file_name" VARCHAR(255),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "deliveries_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "custom_offers" (
    "id" BIGSERIAL NOT NULL,
    "gig_id" INTEGER,
    "sender_id" INTEGER NOT NULL,
    "receiver_id" INTEGER NOT NULL,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT,
    "price" DECIMAL(19,2) NOT NULL,
    "delivery_days" INTEGER NOT NULL,
    "revision_count" INTEGER NOT NULL,
    "status" "CustomOfferStatus" NOT NULL DEFAULT 'PENDING',
    "expires_at" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "custom_offers_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "order_history" ADD CONSTRAINT "order_history_order_id_fkey" FOREIGN KEY ("order_id") REFERENCES "orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "deliveries" ADD CONSTRAINT "deliveries_order_id_fkey" FOREIGN KEY ("order_id") REFERENCES "orders"("id") ON DELETE CASCADE ON UPDATE CASCADE;
